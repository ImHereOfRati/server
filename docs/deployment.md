# 배포 가이드

ImHereServer의 배포 구조, 설정 흐름, 운영 절차를 정리한 문서.

---

## 1. 인프라 구조

물리적으로는 EC2 두 대로 분리되어 있다.

```
┌─────────────────────────────────────────┐    ┌──────────────────────────┐
│ App Server (EC2 #1)                     │    │ Infra Server (EC2 #2)    │
│                                         │    │                          │
│  ┌────────────────┐   ┌─────────────┐   │    │  ┌────────┐  ┌────────┐  │
│  │ Spring Boot    │   │ Prometheus  │   │    │  │ Redis  │  │RabbitMQ│  │
│  │ (port 8080)    │──▶│ (port 9090) │   │    │  │ :6379  │  │ :5672  │  │
│  │ actuator :4861 │   └──────┬──────┘   │    │  └────────┘  └────────┘  │
│  └────────┬───────┘          │          │    │                          │
│           │                  │          │    │  docker-compose.infra.yml│
│           ▼                  ▼          │    └──────────────────────────┘
│  ┌──────────────┐    ┌────────────────┐ │              ▲
│  │ /app/config/ │    │ Grafana Cloud  │ │              │ MySQL (외부)
│  │ /app/secrets/│    │ (remote_write) │ │              │
│  └──────────────┘    └────────────────┘ │
│  docker-compose.prod.yml                │
└─────────────────────────────────────────┘
```

- **App Server**: Spring Boot 앱 + Prometheus (메트릭 수집)
- **Infra Server**: Redis (캐시/세션), RabbitMQ (비동기 알림)
- **MySQL**: 별도 호스팅 (RDS 또는 self-managed)
- **Grafana**: Grafana Cloud로 분리, Prometheus가 remote_write로 푸시

App Server에서 Infra Server / DB로의 접근은 **EC2 보안 그룹**으로 제한한다.

---

## 2. CI/CD 파이프라인

GitHub Actions 두 워크플로로 구성.

| 워크플로                       | 트리거                 | 역할                         |
|----------------------------|---------------------|----------------------------|
| `.github/workflows/ci.yml` | 모든 push, main 대상 PR | 테스트 실행 + JaCoCo 리포트 업로드    |
| `.github/workflows/cd.yml` | CI 성공 후(main 브랜치)   | JAR 빌드 → ECR push → EC2 배포 |

### 2.1 CI 흐름

```
checkout → JDK 21 setup → Gradle cache restore → ./gradlew test
        → 테스트 결과 / JaCoCo 리포트 artifact 업로드
```

`TESTCONTAINERS_RYUK_DISABLED=true` 환경변수로 GitHub runner에서 testcontainers의 Ryuk
컨테이너를 비활성화한다.

### 2.2 CD 흐름

```
1. checkout (CI가 통과한 정확한 commit)
2. JDK 21 + Gradle cache 복원
3. ./gradlew bootJar -x test                          ← secret 파일 없이 빌드
4. AWS 자격증명 + ECR 로그인
5. Docker 이미지 빌드 (Dockerfile.release)
   → ECR push: <repo>:<DATE-SHA> + <repo>:latest
6. GitHub runner의 public IP를 EC2 보안 그룹 22번에 일시 허용
7. SSH key 준비
8. GitHub Secrets에서 application-*.yaml + Firebase 키 → /tmp 에 작성
9. scp로 EC2의 ${EC2_DEPLOY_PATH}/config/, /secrets/ 로 전송
10. EC2에서 `docker compose -f docker-compose.prod.yml pull && up -d`
11. (always) runner IP 보안 그룹에서 제거 + temp 파일 cleanup
```

이미지에는 secret이 들어가지 않으며, EC2 파일시스템의 secret을 컨테이너에 볼륨 마운트한다.

---

## 3. 설정 파일(profile) 구조

Spring Boot 프로파일 로딩 순서:

```
application.yaml              ← 공통 + dev 친화적 디폴트
  ├─ include: secret          → application-secret.yaml      (GitHub Secret)
  ├─ include: monitoring      → application-monitoring.yaml  (GitHub Secret)
  └─ include: datasource      → application-datasource.yaml  (GitHub Secret)

SPRING_PROFILES_ACTIVE=prod   → application-prod.yaml        (GitHub Secret) ← 마지막에 적용
```

마지막 로드되는 프로파일이 우선이므로, `application-prod.yaml`이 모든 위 파일의 값을 덮어쓴다.

### 3.1 각 파일의 역할

| 파일                            | 위치                     | 관리                                  |
|-------------------------------|------------------------|-------------------------------------|
| `application.yaml`            | 이미지 내 (classpath)      | git                                 |
| `application-secret.yaml`     | EC2 `/app/config/` 마운트 | GitHub Secret `APP_SECRET_YAML`     |
| `application-monitoring.yaml` | EC2 `/app/config/` 마운트 | GitHub Secret `APP_MONITORING_YAML` |
| `application-datasource.yaml` | EC2 `/app/config/` 마운트 | GitHub Secret `APP_DATASOURCE_YAML` |
| `application-prod.yaml`       | EC2 `/app/config/` 마운트 | GitHub Secret `APP_PROD_YAML`       |
| `application-test.yml`        | 테스트 classpath          | git (`src/test/resources/`)         |

`.gitignore`로 `application*.yaml` 패턴을 제외하고 있어 secret 파일이 실수로 commit되는 것을 방지한다.

### 3.2 환경별 동작 비교

| 항목                                | 로컬 dev                           | 운영 (prod)                 |
|-----------------------------------|----------------------------------|---------------------------|
| `SPRING_PROFILES_ACTIVE`          | (없음)                             | `prod`                    |
| `ddl-auto`                        | `update` (datasource 기본값)        | `validate` (prod에서 오버라이드) |
| `show-sql`                        | `true`                           | `false`                   |
| `org.springframework.security` 로그 | `TRACE`                          | `INFO`                    |
| p6spy                             | classpath 포함 (`developmentOnly`) | jar에서 제외                  |

**중요 — 이중 안전망**: prod 프로파일이 깜빡 빠지면 dev 디폴트로 동작한다.
이를 막기 위해 `application-datasource.yaml`의 ddl-auto 디폴트는 `validate`로 둔다.

```yaml
ddl-auto: ${JPA_DDL_AUTO:validate}   # 환경변수 미설정 시에도 validate
```

이 설정은 GitHub Secret `APP_DATASOURCE_YAML`에도 동일하게 반영되어야 한다.
로컬 파일만 바꾸면 의미 없으므로 **GitHub Secret 값을 직접 업데이트**해야 함.

---

## 4. 필수 GitHub Secrets

| Secret 이름                                     | 용도                          |
|-----------------------------------------------|-----------------------------|
| `AWS_REGION`                                  | ECR/EC2 리전                  |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | AWS 인증                      |
| `AWS_ECR_REGISTRY`                            | ECR 레지스트리 호스트               |
| `AWS_ECR_REPOSITORY`                          | ECR 리포지토리 이름                |
| `EC2_HOST`                                    | App Server 공개 IP/도메인        |
| `EC2_USER`                                    | SSH 사용자명                    |
| `EC2_SSH_PRIVATE_KEY`                         | SSH 개인키                     |
| `EC2_DEPLOY_PATH`                             | EC2 상의 배포 디렉터리 절대경로         |
| `EC2_SECURITY_GROUP_ID`                       | runner IP 임시 허용용 보안 그룹      |
| `APP_DATASOURCE_YAML`                         | datasource 프로파일 yaml 전문     |
| `APP_MONITORING_YAML`                         | monitoring 프로파일 yaml 전문     |
| `APP_SECRET_YAML`                             | secret 프로파일 yaml 전문         |
| `APP_PROD_YAML`                               | prod 프로파일 yaml 전문           |
| `FIREBASE_JSON_KEY`                           | Firebase Admin SDK 서비스 계정 키 |

### 4.1 `APP_PROD_YAML` 권장 내용

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false

logging:
  level:
    org.springframework.security: INFO
    p6spy: WARN
```

---

## 5. 필수 EC2 설정

### 5.1 App Server `${EC2_DEPLOY_PATH}/.env`

```bash
ECR_REGISTRY=...
ECR_REPOSITORY=...
INFRA_HOST=<infra server IP>
DB_HOST=<MySQL host>
DB_NAME=<schema name>
DB_USER=...
DB_PASSWORD=...
REDIS_PASSWORD=...
RABBITMQ_USER=...
RABBITMQ_PASSWORD=...
```

### 5.2 App Server 디렉터리 구조

CD 워크플로 실행 후 EC2에는 다음 구조가 만들어져 있어야 한다.

```
${EC2_DEPLOY_PATH}/
├── .env
├── docker-compose.prod.yml          ← repo의 동명 파일 사전 배포 필요
├── prometheus.yml                   ← Prometheus scrape 설정
├── config/
│   ├── application-datasource.yaml  ← CD가 매 배포 시 덮어씀
│   ├── application-monitoring.yaml
│   ├── application-secret.yaml
│   └── application-prod.yaml
└── secrets/
    └── imhereFirebaseKey.json       ← CD가 매 배포 시 덮어씀
```

`docker-compose.prod.yml`과 `prometheus.yml`은 CD에서 자동 배포되지 않으므로 **EC2에 미리
한 번 수동으로 올려두어야** 한다.

### 5.3 Infra Server `.env`

```bash
REDIS_PASSWORD=...
RABBITMQ_USER=...
RABBITMQ_PASSWORD=...
```

`docker-compose.infra.yml`로 기동.

---

## 6. 운영 절차

각 절차는 위에서 아래로 순서대로 수행한다.

### 6.1 최초 배포 (zero → 첫 운영)

1. **AWS 사전 준비**
   1. ECR 리포지토리 생성 → 호스트와 리포 이름 기록
   2. App Server / Infra Server EC2 인스턴스 기동 (Amazon Linux 권장)
   3. 보안 그룹 구성:
      - App Server: 80/443(HTTP), 22(SSH는 GitHub runner IP만 동적 허용)
      - Infra Server: 5672/6379는 App Server 보안 그룹만 인바운드 허용
   4. IAM user 발급 (ECR push, EC2 보안 그룹 ingress 권한 필요)

2. **EC2 사전 셋업** (App Server / Infra Server 각각)
   1. Docker + docker compose plugin 설치
   2. App Server에 `${EC2_DEPLOY_PATH}` 디렉터리 생성
   3. 다음 파일을 SCP로 미리 업로드 (CD가 갱신하지 않음):
      - `docker-compose.prod.yml` → `${EC2_DEPLOY_PATH}/`
      - `prometheus.yml` → `${EC2_DEPLOY_PATH}/`
      - Infra Server: `docker-compose.infra.yml`
   4. `${EC2_DEPLOY_PATH}/.env` 작성 (5.1 참고)
   5. Infra Server: `docker compose -f docker-compose.infra.yml up -d` 로 Redis/RabbitMQ 기동

3. **GitHub Secret 등록** (4장 표 14개 모두)
   - repo → Settings → Secrets and variables → Actions → New repository secret
   - 특히 `APP_*_YAML`은 yaml 전문(plain text)을 그대로 붙여 넣음

4. **첫 배포 트리거**
   1. main 브랜치에 push (또는 빈 commit)
   2. Actions 탭에서 CI → CD 순서로 성공 확인
   3. App Server 컨테이너 헬스체크: `curl http://<host>/actuator/health` (관리 포트 노출 시) 또는 SSH로 `docker logs iamhere-server-container`

### 6.2 정상 배포 (반복)

main 브랜치에 merge하면 CI → CD가 자동으로 돌아간다. 추가 작업 없음.

### 6.3 GitHub Secret 변경

1. **로컬에서 새 yaml 작성/검증** (잘못된 yaml은 Spring Boot 기동 자체가 실패하므로 사전 검증 권장)
   - 예: `src/main/resources/application-datasource.yaml` 같은 로컬 사본을 수정해두면 GitHub UI에 그대로 붙여 넣기 쉬움 (gitignored이므로 commit되지 않음)
2. **GitHub UI에서 Secret 갱신**
   1. repo → Settings → Secrets and variables → Actions
   2. 해당 Secret 옆 `Update` → 새 yaml 전체 붙여 넣기 → `Update secret`
3. **CD 재트리거 (반드시 필요)**
   ```bash
   git commit --allow-empty -m "chore: trigger redeploy"
   git push
   ```
4. **반영 확인**: Actions 탭에서 CD 성공 후 EC2 SSH로 `cat ${EC2_DEPLOY_PATH}/config/<해당 파일>` 또는 컨테이너 동작 확인

### 6.4 `APP_DATASOURCE_YAML` ddl-auto 디폴트 변경 (현재 권장 작업)

prod 프로파일이 깜빡 빠져도 안전하도록, datasource yaml의 디폴트값을 `update`에서 `validate`로 바꾼다.

1. **수정 대상 한 줄**
   ```yaml
   # Before
         ddl-auto: ${JPA_DDL_AUTO:update}
   # After
         ddl-auto: ${JPA_DDL_AUTO:validate}
   ```
2. **로컬 파일은 이미 변경 완료** (`src/main/resources/application-datasource.yaml`, gitignored)
3. **GitHub Secret `APP_DATASOURCE_YAML` 갱신** — 위 6.3 절차 따름
4. **CD 재트리거 + 반영 확인**
5. (선택) 운영에서 의도적으로 스키마를 자동 변경해야 할 때만 `JPA_DDL_AUTO=update` 환경변수를 docker-compose에 일시 주입

### 6.5 롤백 (이전 이미지로 복구)

ECR에는 `<DATE-SHA>` 태그로 이전 이미지가 남아있다.

**즉시 롤백 (EC2 직접)**:

1. EC2에 SSH 접속
2. `cd ${EC2_DEPLOY_PATH}`
3. `docker-compose.prod.yml`의 `image: <repo>:latest` 줄을 원하는 `<DATE-SHA>` 태그로 수정
4. `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d`
5. 헬스체크로 동작 확인

**영구 롤백 (main 브랜치 정정)**:

1. main에서 `git revert <문제 commit>` (또는 여러 commit)
2. push → CI → CD 자동 재배포
3. 위 EC2 직접 수정한 image 태그를 다시 `latest`로 되돌림

### 6.6 Prometheus / Grafana

- Prometheus 관리 UI: `http://localhost:9090` (EC2 내부에서 SSH 터널로만 접근 가능, `127.0.0.1` 바인딩)
- Grafana: Grafana Cloud 콘솔 사용
- App actuator: `http://<host>:4861/asdf/prometheus` (관리 포트는 외부 미노출 권장)

### 6.7 트러블슈팅 체크리스트

| 증상 | 우선 확인 |
|---|---|
| CD 실패 (ECR push 단계) | AWS 자격증명 Secret, ECR 리포 권한 |
| CD 성공했는데 컨테이너가 안 뜸 | EC2 SSH → `docker logs iamhere-server-container` → 보통 yaml 파싱 오류 또는 DB 연결 실패 |
| `Could not resolve placeholder` | 해당 secret yaml에 키 누락 또는 `.env` 환경변수 누락 |
| `unknown property` Spring Boot 경고 | application yaml 키 경로 오타 (Spring Boot 4 기준 정식 경로 확인) |
| HTTP 500 (DB 관련) | RDS 보안 그룹, `DB_HOST`/`DB_USER`/`DB_PASSWORD`, `ddl-auto: validate` 시 스키마 불일치 가능 |
| 알림 안 감 (RabbitMQ) | Infra Server `.env`, App Server `.env`의 `INFRA_HOST` 일치 여부, 보안 그룹 5672 인바운드 |

---

## 7. 보안 고려사항

- **이미지에 secret 미포함**: `bootJar`는 `-x test`로 빌드되며 application-*.yaml 중 git에 추적되는 것은 `application.yaml` 1개뿐. 다른 모든
  secret은 EC2 파일시스템에서 볼륨 마운트로 주입.
- **runner IP 제한**: GitHub Actions runner의 IP를 22번 보안 그룹에 **일시적으로만** 허용하고 작업 완료 후 즉시 제거 (`if: always()` 보장).
- **관리 포트 외부 미노출**: actuator(4861), Prometheus(9090), Grafana(3000), RabbitMQ 관리(15672)는 모두 `127.0.0.1` 바인딩.
- **Infra Server 격리**: Redis/RabbitMQ는 App Server에서만 접근 가능하도록 보안 그룹 설정.

---

## 8. 변경 이력

- 2026-05-07: 문서 신설. `application-prod.yaml` 분리 및 production 프로파일 활성화 반영.
- 2026-05-07: 6장(운영 절차) 보강. 최초 배포(6.1), Secret 갱신 절차(6.3), datasource 디폴트 변경(6.4), 트러블슈팅(6.7) 추가.
