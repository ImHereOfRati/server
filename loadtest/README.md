# ImHere 부하 테스트

`loadtest`는 AWS에 임시 부하 테스트 환경을 만들고, k6 시나리오를 실행한 뒤, 테스트가 끝나면 AWS 리소스를 삭제하는 도구 모음이다.

## 실행 구조

```text
로컬 PC
  │
  ├─ run-loadtest.sh
  │    ├─ AWS CloudFormation 실행
  │    ├─ MySQL 설치 및 초기화
  │    ├─ Spring Boot, nginx, Alloy 실행
  │    ├─ k6 fixture와 JWT 생성
  │    ├─ 테스트 시나리오 선택 및 실행
  │    └─ CloudFormation과 EC2 Key Pair 삭제
  │
  └────────────── HTTP 요청 ──────────────▶ AWS 부하 테스트 애플리케이션 EC2
                                             │
                                             ├─ MySQL EC2
                                             └─ 모니터링 EC2
```

부하 테스트 요청은 로컬에서 실행되는 k6가 AWS 애플리케이션 EC2의 Public IP로 보낸다. 따라서 별도의 부하 발생기 EC2는 만들지 않는다.

## AWS 테스트 환경

기본 리전은 `ap-northeast-2`다.

| 구성 | 사양 및 역할 |
|---|---|
| 애플리케이션 EC2 | `t3.small`, Spring Boot + nginx + Alloy |
| 데이터베이스 EC2 | `t3.small`, MySQL Community Server 8.0, 최대 연결 수 30 |
| 모니터링 EC2 | `t3.small`, Grafana + Prometheus + Loki + Tempo |
| 네트워크 | VPC `10.51.0.0/16`, Public Subnet `10.51.1.0/24` |

VPC와 Subnet CIDR은 CloudFormation 파라미터의 기본값이다. 부하 발생기 CIDR과 Grafana 관리자 CIDR은 실행 시 현재 공인 IP를 기준으로 자동 지정된다.

## 사전 요구사항

로컬 환경에 다음 명령이 필요하다.

```bash
aws --version
ssh -V
scp -V
node --version
k6 version
curl --version
```

또한 다음 조건을 만족해야 한다.

- AWS CLI 인증이 완료되어 있어야 한다.
- AWS 리전은 기본적으로 `ap-northeast-2`를 사용한다.
- 운영 CloudFormation 스택 `imhere-prod-infra`의 ECR Repository 출력값이 존재해야 한다.
- ECR에 배포된 애플리케이션 이미지가 있어야 한다.
- 현재 PC의 공인 IP에서 AWS EC2로 SSH 접속할 수 있어야 한다.
- 로컬에 Node.js와 k6가 설치되어 있어야 한다.

AWS 자격 증명은 저장소에 기록하지 않는다.

## 통합 실행

저장소 루트에서 실행한다.

```bash
chmod +x loadtest/run-loadtest.sh
./loadtest/run-loadtest.sh
```

스크립트가 실행되면 다음 순서로 진행된다.

1. 새 EC2 Key Pair 생성
2. CloudFormation으로 VPC와 EC2 생성
3. DB EC2에 MySQL 설치 및 초기화
4. 애플리케이션과 모니터링 서비스 배포
5. k6 테스트 데이터와 JWT 생성
6. `loadtest/k6/test` 아래 테스트 목록 표시
7. 테스트 선택
8. 테스트 계획, RPS, 단계별 실행 시간을 입력
9. k6 실행
10. 테스트 결과 저장
11. CloudFormation 스택, EC2, Key Pair 삭제

테스트 도중 실패하거나 `Ctrl+C`로 중단해도 종료 트랩이 teardown 스크립트를 호출한다. 삭제가 실패한 경우에는 AWS 콘솔에서 `imhere-loadtest` 스택이 남아 있는지 확인해야 한다.

## 테스트 선택

현재 테스트 목록은 다음과 같다.

| 파일 | 용도 |
|---|---|
| `many-people-many-request-test/mixed-workload.js` | 여러 사용자가 조회, FCM, SMS 요청을 함께 수행 |
| `one-to-many-test/user-me.js` | 한 사용자의 `GET /api/users/my` 반복 요청 |
| `one-to-many-test/sms-send.js` | 한 사용자를 대상으로 SMS 알림 요청 반복 |
| `one-to-many-test/fcm-send.js` | 한 사용자를 대상으로 FCM 알림 요청 반복 |
| `one-to-many-test/map-geocode.js` | 지도 주소 변환 API 반복 요청 |

통합 스크립트는 디렉터리를 직접 검색해 `.js` 파일을 메뉴에 표시한다. 새 테스트 파일을 추가하면 별도의 매핑 수정 없이 선택할 수 있다.

## 테스트 계획

통합 스크립트에서 기본값을 그대로 사용하면 다음과 같이 실행된다.

- 테스트 계획: `precision`
- 목표 RPS: `100`
- 단계 실행 시간: `3m`

`mixed-workload.js`의 계획은 다음과 같다.

| 계획 | 동작 |
|---|---|
| `precision` | 100 RPS부터 1000 RPS까지 100 RPS씩 증가, 각 단계 3분 |
| `breakpoint` | 30 RPS부터 1000 RPS까지 증가, 각 단계 3분 |
| `single` | 입력한 목표 RPS를 입력한 시간 동안 실행 |

일부 `one-to-many-test` 시나리오는 내부에 200 → 300 → 400 RPS 단계가 정의되어 있다. 이 테스트에서는 `Stage duration` 값이 각 단계의 실행 시간으로 사용된다.

## 환경 변수

필요한 경우 실행 전에 환경 변수를 지정할 수 있다.

```bash
AWS_REGION=ap-northeast-2 \
STACK_NAME=imhere-loadtest \
IMAGE_TAG=latest \
./loadtest/run-loadtest.sh
```

| 변수 | 기본값 | 설명 |
|---|---|---|
| `AWS_REGION` | `ap-northeast-2` | CloudFormation과 EC2를 생성할 리전 |
| `STACK_NAME` | `imhere-loadtest` | 부하 테스트 CloudFormation 스택 이름 |
| `IMAGE_TAG` | `latest` | ECR에서 가져올 애플리케이션 이미지 태그 |
| `BASE_URL` | 자동 조회 | k6가 호출할 애플리케이션 URL |
| `TEST_PLAN` | `precision` | 기본 테스트 계획 |
| `TARGET_RPS` | `100` | `single` 계획의 목표 RPS |
| `STAGE_DURATION` | `3m` | 단계별 실행 시간 |
| `K6_PATH` | `k6` | k6 실행 파일 경로 |
| `LOAD_GENERATOR_CIDR` | 현재 공인 IP `/32` | 애플리케이션 HTTP/HTTPS 허용 CIDR |
| `OBSERVABILITY_ADMIN_CIDR` | `LOAD_GENERATOR_CIDR` | SSH와 Grafana 접근 허용 CIDR |
| `MYSQL_ROOT_PASSWORD` | 실행 시 입력 | DB root 비밀번호 |

`MYSQL_ROOT_PASSWORD`는 명령행에 직접 작성하지 않는 것을 권장한다.

## k6 초기화 파일

`loadtest/k6/init`에는 테스트용 사용자와 JWT를 만드는 스크립트가 있다.

```bash
node loadtest/k6/init/generate-test-data.mjs
node loadtest/k6/init/issue-jwt.mjs
```

생성되는 파일은 다음과 같다.

| 파일 | 설명 |
|---|---|
| `loadtest/k6/generated/seed.sql` | 테스트용 사용자, 친구 관계, 알림 데이터 SQL |
| `loadtest/k6/generated/fixture.json` | 사용자와 관계 fixture |
| `loadtest/k6/generated/tokens.json` | `JWT_SECRET`로 서명한 테스트용 access token |

통합 실행 스크립트는 이 두 Node.js 스크립트를 자동으로 실행한다. JWT는 애플리케이션 설정의 `loadtest/setup/test-env/server.env`에 있는 `JWT_SECRET`과 같은 키로 서명된다.

애플리케이션 스키마와 테스트 데이터를 직접 적재해야 하는 경우에는 DB 접속 정보에 맞춰 다음 순서로 실행한다.

```bash
mysql -h "$DB_HOST" -u root -p "$DB_NAME" < db/init/mysql/imhere-full-init.sql
mysql -h "$DB_HOST" -u root -p "$DB_NAME" < loadtest/k6/generated/seed.sql
```

## 결과 파일

k6 summary 결과는 다음 위치에 저장된다.

```text
loadtest/k6/generated/results/<test-name>-<test-plan>-<timestamp>.json
```

생성 데이터와 실행 결과는 테스트 실행 산출물이므로 저장소에 커밋하지 않는다.

## 수동 리소스 삭제

통합 스크립트를 사용하지 않고 환경만 삭제할 때는 다음 명령을 사용한다.

```bash
./loadtest/setup/teardown-loadtest.sh --yes
```

`--yes`를 생략하면 전체 AWS 리소스를 삭제하기 전에 확인한다.

## 디렉터리 구조

```text
loadtest/
├── run-loadtest.sh                         # 전체 생성·테스트·삭제 실행
├── k6/
│   ├── init/
│   │   ├── generate-test-data.mjs          # fixture와 seed SQL 생성
│   │   └── issue-jwt.mjs                   # 테스트용 JWT 생성
│   ├── test/                               # k6 테스트 시나리오
│   └── generated/                          # 생성 데이터와 결과
└── setup/
    ├── setup-loadtest.sh                   # AWS 환경 생성 및 서비스 배포
    ├── teardown-loadtest.sh                # AWS 환경 삭제
    ├── infra/
    │   ├── cloudformation/                 # AWS 인프라 템플릿
    │   ├── database/                        # MySQL 설치 스크립트
    │   └── monitoring/                      # Grafana, Loki, Prometheus, Tempo
    └── test-env/                            # 테스트용 애플리케이션 환경 파일
```

## 주의사항

- 부하 테스트가 끝나면 반드시 AWS 리소스가 삭제됐는지 확인한다.
- 테스트 중 생성한 EC2 Key Pair의 private key는 `loadtest/setup/info/keys` 아래에 임시 저장된다.
- `test-env`의 값은 테스트 전용 가짜 값이지만, 실제 운영용 credential을 넣지 않는다.
- 테스트 대상 URL과 CIDR을 잘못 지정하면 요청이 차단되거나 의도하지 않은 서버로 부하가 전달될 수 있다.
- `latest` 태그를 사용할 경우 ECR의 최신 이미지가 pull된다. 특정 배포본을 검증하려면 `IMAGE_TAG`를 명시한다.
