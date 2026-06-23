# Deployment

## 한 줄 요약

- 로컬: `docker-compose.yml`
- 인프라(Redis/RabbitMQ): `docker-compose.infra.yml`
- 운영: `docker-compose.prod.yml`
- 빌드: `Dockerfile` (로컬용) / `Dockerfile.release` (CI/CD용)

## Docker 구성

### `Dockerfile` (로컬/검증용)
멀티 스테이지(`builder` + `runtime`)로 `./gradlew build -x test`를 돌려 JAR를 만들고 실행 이미지에는 JAR만 남긴다.

### `Dockerfile.release` (운영용)
CI가 만든 JAR(`build/libs/*.jar`)를 그대로 `app.jar`로 복사한다. Gradle 빌드 단계가 없어 더 단순하고, CI에서 검증된 JAR를 재사용한다. `8080`, `4861` 포트를 노출한다.

### `docker-compose.yml` (로컬)
서비스: `app`(Spring Boot), `prometheus`, `grafana`. 앱 포트는 `80:8080`, DB/Redis/RabbitMQ는 환경변수로 외부 연결.

### `docker-compose.infra.yml` (인프라)
서비스: `redis`, `rabbitmq`. Redis는 비밀번호 필수, RabbitMQ 관리 UI는 로컬에만 노출.

### `docker-compose.prod.yml` (운영)
서비스: `dsko`(Spring Boot), `nginx`, `alloy`. ECR의 `latest` 이미지를 쓰고 `config/`, `secrets/`를 마운트한다. Redis/RabbitMQ/MySQL은 이 compose 밖의 별도 인프라에서 제공된다.

### 포트 정리
`8080`(Spring) · `4861`(관리/모니터링) · `80`/`443`(Nginx) · `9090`(Prometheus) · `3000`(Grafana) · `5672`(RabbitMQ AMQP) · `15672`(RabbitMQ UI) · `6379`(Redis)

## Nginx

`nginx/nginx.conf` 핵심:
- 443(HTTPS)만 실제 응답, 80은 443으로 301 리다이렉트, 그 외 Host는 404.
- Let's Encrypt(Certbot) 인증서를 `/etc/letsencrypt`에서 마운트.
- CORS는 `https://fortuneki.site`만 허용, OPTIONS는 204로 즉시 응답.
- `proxy_pass http://dsko:8080`, `proxy_hide_header Access-Control-Allow-Origin`으로 Spring 쪽 중복 CORS 헤더를 제거.
- 인증서 갱신: `certbot renew` 후 `docker exec nginx-container nginx -s reload` (cron 등록 권장, 만료 90일/갱신 권장 30일 전).

## CI (`IMHERE_GITHUB_ACTION_CI`)

- 트리거: 모든 브랜치 push, `main` 대상 PR.
- `./gradlew test`만 실행한다(운영 배포는 하지 않음). `TESTCONTAINERS_RYUK_DISABLED=true`로 Testcontainers 안정화.
- 산출물: 테스트 리포트, JaCoCo 커버리지 리포트.
- CI 통과가 CD 시작 조건이다.

## CD (`IMHERE_GITHUB_ACTION_CD`)

- 트리거: `IMHERE_GITHUB_ACTION_CI`의 `workflow_run` 성공 시(main).
- **build-jar**: CI와 같은 커밋으로 `./gradlew bootJar -x test` → JAR 아티팩트(1일 보관).
- **docker-push**: OIDC로 AWS 역할 assume(장기 Access Key 없음) → `Dockerfile.release`로 이미지 빌드 → ECR에 날짜-SHA 태그 + `latest`로 푸시.
- **deploy**: 러너 공인 IP를 EC2 보안 그룹에 임시 허용 → SSH로 설정 파일(`application-*.yaml`, Firebase 키)을 EC2에 전송 → ECR pull → `docker compose -f docker-compose.prod.yml up -d` → 보안 그룹 규칙/임시 파일 정리.

## 환경 변수 (이름만 — 값은 GitHub Secrets/EC2 `.env`에만 존재)

| 분류 | 변수 |
|---|---|
| AWS | `AWS_REGION`, `AWS_ECR_REGISTRY`, `AWS_ECR_REPOSITORY`, `AWS_DEPLOY_ROLE_ARN`, `EC2_SECURITY_GROUP_ID` |
| EC2 | `EC2_HOST`, `EC2_USER`, `EC2_SSH_PRIVATE_KEY`, `EC2_DEPLOY_PATH` |
| 운영 설정 파일(payload) | `APP_DATASOURCE_YAML`, `APP_MONITORING_YAML`, `APP_SECRET_YAML`, `APP_PROD_YAML`, `FIREBASE_JSON_KEY` |
| EC2 런타임 `.env` | `ECR_REGISTRY`, `ECR_REPOSITORY`, `INFRA_HOST`, `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `REDIS_PASSWORD`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`, `GRAFANA_CLOUD_*` |

## AWS

| 리소스 | 역할 |
|---|---|
| EC2(앱) | `dsko` + `nginx` + `alloy` 실행 |
| EC2(infra) | Redis + RabbitMQ 실행 |
| ECR | 운영 이미지 저장소 |
| IAM Role(OIDC) | GitHub Actions가 장기 키 없이 assume |
| Security Group | 배포 시점에만 러너 IP의 SSH(22) 임시 허용, 끝나면 회수 |

## 도메인 / DB 호스팅 (가비아)

- 도메인 `fortuneki.site`의 DNS A 레코드를 EC2 퍼블릭 IP로 연결한다.
- MySQL은 AWS RDS가 아니라 가비아에서 별도로 호스팅한다.
- TLS 인증서는 가비아가 아니라 Let's Encrypt(Certbot)에서 발급한다 — 가비아는 도메인 소유만 증명한다.

## 로컬 실행

```bash
# 인프라만 (Redis, RabbitMQ)
docker compose -f docker-compose.infra.yml up -d

# 앱 (+ Prometheus, Grafana)
docker compose up -d

# 또는 직접 실행
./gradlew bootRun

# 테스트
./gradlew test
```
