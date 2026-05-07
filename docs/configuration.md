# 설정 및 환경변수 가이드

ImHereServer의 Spring Boot 프로파일 구조와 환경별 설정 관리 방식을 정리한 문서입니다.

---

## 1. Spring Boot 프로필

프로젝트는 계층 구조를 가진 멀티 프로파일 전략을 사용합니다.

### 1.1 로딩 순서

가장 마지막에 로드되는 프로파일이 이전 설정값을 덮어씁니다.

1. `application.yaml` (공통)
2. `application-secret.yaml` (시크릿)
3. `application-monitoring.yaml` (모니터링)
4. `application-datasource.yaml` (DB 설정)
5. `application-prod.yaml` (운영 전용 - 최우선)

### 1.2 프로파일별 역할

| 파일                            | 위치             | 특징                               |
|-------------------------------|----------------|----------------------------------|
| `application.yaml`            | Classpath      | 기본 설정 및 개발 친화적 디폴트 값 포함 (Git 추적) |
| `application-secret.yaml`     | External       | API 키, JWT 비밀키 등 민감 정보 (Git 제외)  |
| `application-monitoring.yaml` | External       | Actuator 포트, 메트릭 수집 경로 설정        |
| `application-datasource.yaml` | External       | DB 연결 정보 및 JPA 설정                |
| `application-test.yml`        | Test Classpath | 테스트 환경 전용 설정 (H2, Mock 등)        |

---

## 2. 운영 환경 설정 (.env)

EC2 운영 서버에서는 Docker Compose를 통해 환경변수를 주입합니다. `${EC2_DEPLOY_PATH}/.env` 파일에 다음 항목이 정의되어야 합니다.

### 2.1 기본 인프라

- `ECR_REGISTRY`, `ECR_REPOSITORY` (이미지 풀)
- `INFRA_HOST` (Redis / RabbitMQ가 동작하는 미들웨어 EC2 호스트)
- `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_PASSWORD`
- `RABBITMQ_USER`, `RABBITMQ_PASSWORD`

### 2.2 Grafana Cloud 자격증명

- `GRAFANA_CLOUD_LOKI_*`: 로그 전송용 — `_ENDPOINT`, `_USER`, `_API_KEY`
- `GRAFANA_CLOUD_PROM_*`: 메트릭 전송용 — `_ENDPOINT`, `_USER`, `_API_KEY`
- `GRAFANA_CLOUD_TEMPO_*`: 트레이스 전송용 — `_ENDPOINT`, `_AUTH_BASE64`

### 2.3 `.env` 예시

실제 값은 절대 커밋하지 않습니다. 아래는 형식을 보이기 위한 더미 예시이며, EC2의 `${EC2_DEPLOY_PATH}/.env`에 그대로 두면 `docker compose`가 자동으로 로드합니다.

```dotenv
# --- Image / Infra 호스트 ---
ECR_REGISTRY=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com
ECR_REPOSITORY=imhere/dsko
INFRA_HOST=10.0.1.20                # 미들웨어 EC2 (Redis / RabbitMQ)

# --- Datastore ---
DB_HOST=10.0.1.10                   # 데이터베이스 EC2
DB_NAME=imhere
DB_USER=imhere_app
DB_PASSWORD=change-me-strong-password
REDIS_PASSWORD=change-me-redis-password
RABBITMQ_USER=imhere
RABBITMQ_PASSWORD=change-me-rabbit-password

# --- Grafana Cloud: Loki (logs, via Alloy) ---
GRAFANA_CLOUD_LOKI_ENDPOINT=https://logs-prod-XXX.grafana.net/loki/api/v1/push
GRAFANA_CLOUD_LOKI_USER=1234567
GRAFANA_CLOUD_LOKI_API_KEY=glc_eyJvIjoiMTIzNDU2NyIsIm4iOiJsb2tpLXdyaXRlIiwiayI6...

# --- Grafana Cloud: Prometheus (metrics, via Alloy) ---
GRAFANA_CLOUD_PROM_ENDPOINT=https://prometheus-prod-XX-prod-ap-northeast-0.grafana.net/api/prom/push
GRAFANA_CLOUD_PROM_USER=1234567
GRAFANA_CLOUD_PROM_API_KEY=glc_eyJvIjoiMTIzNDU2NyIsIm4iOiJwcm9tLXdyaXRlIiwiayI6...

# --- Grafana Cloud: Tempo (traces, App에서 OTLP 직접 push) ---
GRAFANA_CLOUD_TEMPO_ENDPOINT=https://tempo-prod-XX-prod-ap-northeast-0.grafana.net/otlp
# "user:api_key" 형식을 base64 인코딩한 값 (echo -n "user:key" | base64)
GRAFANA_CLOUD_TEMPO_AUTH_BASE64=MTIzNDU2NzpnbGNfZXlKdklqb2lNVEl6TkRVMk55SXNJbTRpT2lKMFpXMXdieTEzY21sMFpTSXNJbXNpT2k0Li4u
```

> **주의**:
> - 파일 권한은 `chmod 600 .env` 로 제한하고, 소유자는 `docker compose` 실행 계정으로 둡니다.
> - `GRAFANA_CLOUD_TEMPO_AUTH_BASE64`는 OTLP HTTP `Authorization: Basic` 헤더에 그대로 붙여 쓰는 값이라, `<user>:<api_key>`를 base64 인코딩한 결과여야 합니다. (Loki/Prom은 Alloy가 user/key를 분리해서 받기 때문에 base64가 아닌 평문)

---

## 3. 주요 보안 설정

### 3.1 JPA ddl-auto

실수로 인한 데이터 유실을 방지하기 위해 이중 안전장치를 둡니다.

- **기본값**: `validate` (`application-datasource.yaml`에 명시)
- **로컬/개발**: 필요한 경우에만 환경변수(`JPA_DDL_AUTO`)로 `update` 주입.

### 3.2 로깅 레벨

- **운영**: `INFO` 레벨을 기본으로 하며, 보안 및 성능 로그는 구조화된 JSON으로 기록합니다.
- **로그 보존**: 중요 에러 로그는 별도 파일로 90일간 보관합니다.
