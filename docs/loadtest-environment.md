---
# 수정 필요
---
---

# ImHere 부하 테스트 환경

이 문서는 [`loadtest/README.md`](../loadtest/README.md)를 기준으로 부하 테스트 환경의 구성과 운영 방식을 설명한다. 실제 실행은 README의 통합 스크립트를 기준으로 한다.

## 1. 목적

부하 테스트 환경은 운영 환경과 분리된 임시 AWS 환경이다. 로컬 PC에서 k6를 실행해 AWS의 Spring Boot 애플리케이션으로 요청을 보내고, 테스트가 끝나면 CloudFormation으로 생성한 리소스를 모두 삭제한다.

부하 발생기 EC2는 별도로 만들지 않는다. 부하 발생기는 로컬 PC의 k6 프로세스다.

```text
로컬 PC
  └─ k6 ── HTTP/HTTPS ──▶ 애플리케이션 EC2
                            ├─ Spring Boot
                            ├─ nginx
                            └─ Alloy
                                  │
                                  └─▶ 모니터링 EC2
                                      ├─ Prometheus
                                      ├─ Loki
                                      ├─ Tempo
                                      └─ Grafana

애플리케이션 EC2 ── MySQL ──▶ DB EC2
```

## 2. AWS 리소스

기본 리전은 `ap-northeast-2`다.

| 리소스 | 구성 |
|---|---|
| 애플리케이션 EC2 | Amazon Linux 2023, `t3.small` (2 vCPU, 2 GiB), Spring Boot + nginx + Alloy |
| 데이터베이스 EC2 | Amazon Linux 2023, `t3.small` (2 vCPU, 2 GiB), MySQL Community Server 8.0 |
| 모니터링 EC2 | Amazon Linux 2023, `t3.small` (2 vCPU, 2 GiB), Grafana + Prometheus + Loki + Tempo |
| VPC | 기본 CIDR `10.51.0.0/16` |
| Public Subnet | 기본 CIDR `10.51.1.0/24`, Public IP 자동 할당 |
| 인터넷 연결 | Internet Gateway와 기본 경로 `0.0.0.0/0` |

MySQL은 Docker가 아니라 DB EC2 운영체제에 직접 설치한다. `max_connections`는 `30`으로 설정한다.

## 3. 네트워크와 보안 그룹

CloudFormation 템플릿은 [`load-test-aws-setup.yaml`](../loadtest/setup/infra/cloudformation/load-test-aws-setup.yaml)이다.

| 보안 그룹 | 포트 | 허용 출처 | 용도 |
|---|---:|---|---|
| App | 22 | `ObservabilityAdminCidr` | 관리자 SSH |
| App | 80, 443 | `LoadGeneratorCidr` | 로컬 k6 요청 |
| DB | 22 | `ObservabilityAdminCidr` | MySQL 설치와 점검 |
| DB | 3306 | App Security Group | 애플리케이션의 MySQL 접속 |
| Observability | 22 | `ObservabilityAdminCidr` | 관리자 SSH |
| Observability | 3000 | `ObservabilityAdminCidr` | Grafana UI |
| Observability | 9090, 3100, 4317, 4318 | App Security Group | 애플리케이션 관측 데이터 수집 |

`LoadGeneratorCidr`는 k6를 실행하는 PC의 공인 IP `/32`로 자동 설정된다. `ObservabilityAdminCidr`를 지정하지 않으면 같은 값이 사용된다.

즉, 현재 PC의 공인 IP가 바뀌면 기존 환경을 재사용하지 말고 새 환경을 생성하거나 CIDR을 다시 지정해야 한다.

## 4. 애플리케이션 구성

애플리케이션 EC2에는 다음을 실행한다.

- 운영 `docker-compose.yml`
- `loadtest/setup/infra/docker-compose.loadtest.yml` override
- Spring 환경 파일과 테스트용 secret
- nginx 설정
- Alloy 설정

애플리케이션 이미지는 로컬에서 빌드하지 않는다. GitHub Actions의 CD 파이프라인이 ECR에 push한 이미지를 pull한다.

```text
${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
```

기본 이미지 태그는 `latest`이며, `IMAGE_TAG` 환경 변수로 특정 태그를 지정할 수 있다. Compose는 `pull_policy: always`를 사용한다.

Spring 프로필은 테스트용 설정과 운영 공통 설정을 함께 사용하기 위해 `prod,loadtest`를 활성화한다. `loadtest` 프로필에서는 외부 FCM/SMS provider를 실제로 호출하지 않는 테스트용 adapter를 사용한다.

## 5. 모니터링 구성

모니터링 EC2에는 다음 컨테이너가 실행된다.

| 컴포넌트 | 역할 |
|---|---|
| Grafana | 대시보드와 데이터 조회 UI |
| Prometheus | 애플리케이션 메트릭 저장 |
| Loki | 애플리케이션 로그 저장 |
| Tempo | Trace 저장 |
| Alloy | 애플리케이션의 메트릭·로그·Trace 전달 |

모니터링 설정은 [`loadtest/setup/infra/monitoring`](../loadtest/setup/infra/monitoring)에 있다. Grafana는 관리자 CIDR에서만 `3000` 포트로 접근할 수 있다.

## 6. 전체 실행 흐름

권장 실행 명령은 저장소 루트에서 다음과 같다.

```bash
chmod +x loadtest/run-loadtest.sh
./loadtest/run-loadtest.sh
```

[`run-loadtest.sh`](../loadtest/run-loadtest.sh)는 다음 작업을 순서대로 수행한다.

1. 매 실행마다 새 EC2 Key Pair 생성
2. CloudFormation으로 VPC, Subnet, 보안 그룹, EC2 생성
3. DB EC2에 MySQL 8.0 직접 설치 및 초기화
4. 애플리케이션과 모니터링 설정 파일 전송
5. ECR 로그인과 Docker 이미지 pull
6. Spring Boot, nginx, Alloy, Grafana 스택 실행
7. k6 fixture와 JWT 생성
8. `loadtest/k6/test` 아래 테스트 목록 표시
9. 사용자가 테스트, 계획, 목표 RPS, 실행 시간을 선택
10. k6 실행 결과 저장
11. 종료 트랩으로 CloudFormation 스택과 EC2 Key Pair 삭제

테스트가 성공한 경우뿐 아니라 k6 실패, 초기화 실패, `Ctrl+C` 중단에도 teardown을 시도한다. teardown이 실패하면 AWS 콘솔에서 `imhere-loadtest` 스택을 직접 확인해야 한다.

## 7. k6 테스트 데이터

초기화 스크립트는 [`loadtest/k6/init`](../loadtest/k6/init)에 있다.

```bash
node loadtest/k6/init/generate-test-data.mjs
node loadtest/k6/init/issue-jwt.mjs
```

생성 결과는 `loadtest/k6/generated`에 저장된다.

| 파일 | 내용 |
|---|---|
| `seed.sql` | 테스트 사용자 1,000명, 친구 관계, 알림 데이터 SQL |
| `fixture.json` | 테스트 사용자와 관계 fixture |
| `tokens.json` | `JWT_SECRET`로 서명한 access token |
| `results/*.json` | k6 summary 결과 |

`issue-jwt.mjs`는 `loadtest/setup/test-env/app.env`의 `JWT_SECRET`을 읽어 토큰을 만든다. 통합 스크립트는 초기화 두 단계를 자동으로 실행한다.

스키마나 fixture 데이터를 직접 적재할 때는 다음 순서를 사용한다.

```bash
mysql -h "$DB_HOST" -u root -p "$DB_NAME" < db/init/mysql/imhere-full-init.sql
mysql -h "$DB_HOST" -u root -p "$DB_NAME" < loadtest/k6/generated/seed.sql
```

## 8. 테스트 시나리오

테스트 파일은 [`loadtest/k6/test`](../loadtest/k6/test)에 둔다.

| 시나리오 | 대상 |
|---|---|
| `mixed-workload.js` | 조회, FCM, SMS를 섞은 다중 사용자 부하 |
| `user-me.js` | 한 사용자의 `GET /api/users/my` |
| `sms-send.js` | 한 사용자 대상 SMS 알림 |
| `fcm-send.js` | 한 사용자 대상 FCM 알림 |
| `map-geocode.js` | 지도 주소 변환 API |

통합 스크립트는 `.js` 파일을 자동 검색하므로 새 시나리오를 추가해도 별도 매핑이 필요하지 않다.

`mixed-workload.js`의 테스트 계획은 다음과 같다.

| 계획 | 동작 |
|---|---|
| `precision` | 100~1000 RPS를 100 RPS씩 증가, 각 단계 3분 |
| `breakpoint` | 30~1000 RPS를 증가시켜 한계점 확인 |
| `single` | 지정한 RPS를 지정한 시간 동안 실행 |

## 9. 주요 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `AWS_REGION` | `ap-northeast-2` | AWS 리전 |
| `STACK_NAME` | `imhere-loadtest` | CloudFormation 스택 이름 |
| `IMAGE_TAG` | `latest` | ECR 이미지 태그 |
| `BASE_URL` | App Public IP 자동 조회 | k6 대상 URL |
| `TEST_PLAN` | `precision` | 기본 테스트 계획 |
| `TARGET_RPS` | `100` | `single` 계획의 목표 RPS |
| `STAGE_DURATION` | `3m` | 단계별 실행 시간 |
| `K6_PATH` | `k6` | k6 실행 파일 또는 절대 경로 |
| `LOAD_GENERATOR_CIDR` | 현재 공인 IP `/32` | App HTTP/HTTPS 허용 CIDR |
| `OBSERVABILITY_ADMIN_CIDR` | `LOAD_GENERATOR_CIDR` | SSH와 Grafana 허용 CIDR |
| `MYSQL_ROOT_PASSWORD` | 실행 시 입력 | MySQL root 비밀번호 |

비밀번호와 credential은 명령 기록이나 저장소에 남기지 않는다.

## 10. 리소스 삭제

통합 스크립트는 테스트 종료 시 자동으로 삭제한다. 환경만 수동으로 삭제하려면 다음을 실행한다.

```bash
./loadtest/setup/teardown-loadtest.sh --yes
```

teardown은 다음을 삭제한다.

- CloudFormation 스택
- VPC, Subnet, Internet Gateway, Route Table
- 애플리케이션, DB, 모니터링 EC2
- CloudFormation으로 생성된 보안 그룹
- 실행 시 생성한 EC2 Key Pair
- 로컬 `.loadtest-state`와 활성 private key

## 11. 파일 구조

```text
loadtest/
├── README.md
├── run-loadtest.sh
├── k6/
│   ├── init/
│   ├── test/
│   └── generated/
└── setup/
    ├── setup-loadtest.sh
    ├── teardown-loadtest.sh
    ├── infra/
    │   ├── cloudformation/
    │   ├── database/
    │   └── monitoring/
    └── test-env/
```

주요 경로는 다음과 같다.

| 경로 | 역할 |
|---|---|
| `setup/setup-loadtest.sh` | AWS 생성과 서비스 배포 |
| `setup/teardown-loadtest.sh` | AWS 전체 삭제 |
| `setup/infra/cloudformation/` | 인프라 템플릿 |
| `setup/infra/database/setup-mysql.sh` | MySQL 직접 설치 스크립트 |
| `setup/infra/monitoring/` | 모니터링 Compose와 설정 |
| `setup/test-env/` | 테스트용 애플리케이션 환경 파일 |
| `k6/init/` | fixture와 JWT 생성 |
| `k6/test/` | k6 시나리오 |
| `k6/generated/` | 생성 데이터와 결과 |

## 12. 주의사항

- 부하 테스트가 끝난 뒤 CloudFormation 스택이 실제로 삭제됐는지 확인한다.
- `loadtest/setup/info/keys`의 private key는 임시 키이며, AWS 환경 삭제 후 함께 삭제된다.
- `test-env`에는 테스트용 가짜 값만 사용하고 운영 credential을 복사하지 않는다.
- `BASE_URL`을 잘못 지정하면 의도하지 않은 서버에 부하가 전달될 수 있다.
- `LOAD_GENERATOR_CIDR`과 `OBSERVABILITY_ADMIN_CIDR`은 필요한 IP만 허용하도록 설정한다.
- `t3.small`은 CPU와 메모리가 제한적이므로 JVM, Docker, 모니터링 컨테이너의 리소스 사용량을 함께 확인한다.
- `latest` 대신 특정 ECR 태그를 사용하면 테스트 대상 이미지가 명확해진다.
---
# 수정 필요
---
---
