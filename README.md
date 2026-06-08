# ImHere — Server

**ImHere**는 위치 기반 알림 서비스의 백엔드 서버입니다.
사용자가 특정 위치에 도착하거나 이탈할 때 지정된 연락처로 알림을 자동 전송합니다.

연동 클라이언트: [ImHere AOS](https://github.com/kdongsu5509/ImHere-AOS) *(Android)*

---

## 기술 스택

| 분류            | 기술                                        |
|---------------|-------------------------------------------|
| Language      | Kotlin 2.x                                |
| Runtime       | Java 21                                   |
| Framework     | Spring Boot 4.0.x                         |
| Architecture  | Hexagonal (Ports & Adapters)              |
| DB            | MySQL + Spring Data JPA + QueryDSL (KSP)  |
| Cache         | Redis                                     |
| Message Queue | RabbitMQ                                  |
| Auth          | Kakao OIDC + JJWT + Spring Security       |
| Admin Auth    | Spring Security OTT (One-Time Token)      |
| Push          | Firebase Admin SDK (FCM)                  |
| SMS           | Solapi SDK                                |
| Alerting      | Discord Webhook                           |
| API Docs      | Spring REST Docs + OpenAPI 3 (Swagger UI) |
| Monitoring    | Micrometer + Prometheus                   |
| Test          | JUnit 5, Mockito-Kotlin, Testcontainers   |
| Coverage      | JaCoCo                                    |
| Infra         | AWS EC2 + ECR, Docker                     |

---

## 아키텍처

비즈니스 로직의 변경 빈도와 복잡도에 따라 두 가지 아키텍처 중 하나를 선택적으로 적용하는 **하이브리드 아키텍처**를 사용합니다.

1. **MVC (Layered Architecture)** (`user`, `friends`, `terms` 도메인)
    - 변경이 적고 단순한 CRUD 중심의 도메인에 적용
    - `Controller` -> `Service` -> `Repository` 계층형 구조
2. **Hexagonal Architecture (Ports and Adapters)** (`auth`, `notifications` 도메인)
    - 외부 시스템 연동이 잦고 복잡한 비즈니스 로직이 포함된 도메인에 적용
    - 프레임워크 의존성 없이 순수한 도메인/애플리케이션 레이어를 유지하며 외부 의존성은 `adapter`로 분리

```
src/main/kotlin/com/kdongsu5509/
├── auth/                          # 인증 도메인 (Hexagonal)
├── friends/                       # 친구 도메인 (MVC)
├── notifications/                 # 알림 도메인 (Hexagonal)
├── terms/                         # 약관 도메인 (MVC)
├── user/                          # 사용자 도메인 (MVC)
├── shared/                        # 도메인 간 공통 객체 (BaseEntity 등)
└── support/                       # 횡단 관심사
    ├── config/                    # Security, Redis, RabbitMQ 설정
    ├── exception/                 # 글로벌 예외 처리, 에러 코드
    ├── external/                  # 외부 시스템 연동 (Discord Webhook 등)
    ├── logger/                    # HTTP 접근 로그, 데이터 마스킹
    └── response/                  # 공통 API 응답 래퍼
```

---

## 주요 기능

### 인증

- **Kakao OIDC 로그인** — 카카오 공개키 검증 후 자체 JWT 발급
- **JWT 재발급** — Redis 기반 Refresh Token 관리
- **어드민 OTT 로그인** — Spring Security One-Time Token + Discord 알림
- 카카오 공개키 7일 주기 자동 갱신 (스케쥴러 + Redis 캐시)

### 사용자 / 친구

- 사용자 닉네임 조회/수정
- 친구 요청 · 수락 · 거절
- 친구 차단/해제
- 이용 약관 동의

### 알림

- **FCM 푸시 알림** — 위치 도착/출발 알림 (Retryable 재시도 포함)
- **SMS 알림** — Solapi 단일/다중 발송
- RabbitMQ를 통한 비동기 알림 전달 (메시지 멱등성 보장 및 DLQ 관리자 기능 포함)

### 운영

- **HTTP 접근 로그** — MDC(traceId/method/uri/status/durationMs) + ECS JSON 구조화 로그
- **에러 로그 분리** — WARN 이상 전용 롤링 파일 (90일 보존)
- **Discord 알림** — 5xx 서버 에러 즉시 알림 (비동기 발송)
- **API 문서** — Spring REST Docs 기반 OpenAPI 3 spec + Swagger UI (`/swagger-ui/index.html`)
- **모니터링** — Prometheus 메트릭 (`/actuator/prometheus`)

---

## 로컬 실행

### 사전 요구사항

- JDK 21
- Docker & Docker Compose

### 인프라 실행

```bash
docker-compose up -d redis
```

> MySQL, RabbitMQ는 별도 설치 또는 컨테이너로 실행 필요

### 환경 변수 설정

`src/main/resources/application-secret.yaml` 생성:

```yaml
jwt:
  secret: <32자 이상 랜덤 문자열>
  access-expiration-minutes: 30
  refresh-expiration-days: 7
  access-header-name: Authorization

oidc:
  kakao:
    issuer: "https://kauth.kakao.com"
    audience: <카카오 REST API 키>
    cacheKey: "kakaoOidcKeys::kakaoPublicKeySet"

discord:
  url:
    error: <5xx 에러 알림 Discord Webhook URL>
    ott: <어드민 OTT 토큰 알림 Discord Webhook URL>

security:
  whitelist:
    - "/api/user/auth/login"
    - "/swagger-ui/**"
    - "/v3/api-docs/**"
  admin:
    secret: <어드민 요청 헤더 시크릿>
```

### 빌드 및 실행

```bash
./gradlew bootRun
```

서버 기본 포트: `8080`
API 문서: `http://localhost:8080/swagger-ui/index.html`

---

## 테스트

```bash
# 전체 테스트 + 커버리지 리포트 + API 문서 생성
./gradlew test

# 커버리지 리포트 확인
open build/jacocoHtml/index.html
```

> Testcontainers를 사용하므로 Docker 데몬이 실행 중이어야 합니다.

---

## 배포

### Docker 이미지 빌드

```bash
docker build -t iamhere:latest .
```

### AWS ECR 푸시

```bash
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin <ECR_REGISTRY>

docker tag iamhere:latest <ECR_REGISTRY>/iamhere:latest
docker push <ECR_REGISTRY>/iamhere:latest
```

### 운영 배포

```bash
docker-compose up -d
```


