# 📍 ImHere Server

ImHere Server는 위치 기반 알림 서비스의 백엔드다.
사용자가 지정한 위치에 도착하거나 이탈하면, 등록된 대상에게 알림을 보낸다.

모바일 클라이언트는 Flutter로 만든 ImHere이며, 설치는 [플레이스토어](https://play.google.com/store/apps/details?id=com.kdongsu5509.iamhere)에서
한다.

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
</p>
<p>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Caffeine-FF9F1C?style=for-the-badge&logo=coffeescript&logoColor=white" alt="Caffeine"/>
  <img src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" alt="RabbitMQ"/>
  <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
</p>
<p>
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/Amazon%20AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white" alt="AWS"/>
  <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white" alt="Nginx"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white" alt="GitHub Actions"/>
</p>
<p>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase"/>
  <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white" alt="Grafana"/>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black" alt="Swagger/OpenAPI"/>
  <img src="https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white" alt="JUnit5"/>
</p>

---

## 개요

- 위치 기반 도착/이탈 알림 처리
- Kakao/Google OIDC 로그인
- JWT 발급 및 Refresh Token 재발급
- 사용자, 친구, 약관, 알림, 기록, 운영 도메인 분리
- RabbitMQ 기반 비동기 알림 처리
- FCM, SMS, Discord, Grafana Cloud 연동
- REST Docs + OpenAPI 기반 API 문서화
- 로그, 메트릭, 트레이스, 에러 알림 운영

---

## 기술 스택

| 분류            | 기술                                                 |
|---------------|----------------------------------------------------|
| Language      | Kotlin                                             |
| Runtime       | Java                                               |
| Framework     | Spring Boot                                        |
| Architecture  | Hybrid MVC + Hexagonal                             |
| DB            | MySQL, Spring Data JPA, QueryDSL                   |
| Cache         | Caffeine                                           |
| Queue         | RabbitMQ                                           |
| Auth          | Kakao OIDC, Google OIDC, JWT, Spring Security      |
| Admin Auth    | Spring Security OTT                                |
| Push          | Firebase Admin SDK (FCM)                           |
| SMS           | Solapi SDK                                         |
| Alerting      | Discord Webhook                                    |
| API Docs      | Spring REST Docs, OpenAPI 3                        |
| Observability | Micrometer, Prometheus, Grafana Alloy, Loki, Tempo |
| Test          | JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers |
| Coverage      | JaCoCo                                             |
| Infra         | AWS EC2, ECR, Docker                               |

---

## 서비스 도메인

### 인증

- Kakao OIDC 로그인
- Google OIDC 로그인
- JWT 발급/재발급
- Caffeine 기반 공개키 캐시
- 어드민 OTT 로그인

### 사용자

- 회원 정보 조회/수정
- 활성화/비활성화 흐름

### 친구

- 친구 요청, 수락, 거절
- 차단, 제한, 해제

### 약관

- 필수 약관 동의
- 약관 버전 관리

### 알림

- FCM 푸시
- SMS 발송
- RabbitMQ 비동기 큐
- 멱등성 및 DLQ 재시도

### 운영

- 구조화 로그
- 에러 코드 체계
- 모니터링/트레이싱
- 배포/롤백/인증서 운영

---

## 아키텍처

프로젝트는 하이브리드 구조를 사용한다.

### MVC

단순 CRUD 성격의 도메인에 적용한다.

- `user`
- `friends`
- `terms`

### Hexagonal

외부 연동이 많고 비즈니스 복잡도가 높은 도메인에 적용한다.

- `auth`
- `notifications`

### 패키지 구조

```text
src/main/kotlin/com/kdongsu5509/
├── auth/
├── friends/
├── notifications/
├── terms/
├── user/
├── shared/
└── support/
    ├── config/
    ├── exception/
    ├── external/
    ├── logger/
    └── response/
```

### 레이어 책임

- Controller: HTTP 입출력, 검증, DTO 변환
- Service: 유스케이스, 트랜잭션, 비즈니스 로직
- Repository: 영속성 접근
- Domain: 순수 비즈니스 규칙

---

## 데이터 모델

### 공통 베이스

- `BaseTimeEntity`: `createdAt`, `updatedAt`
- `BaseEntity`: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`

### 테이블 요약

| 테이블                    | 도메인           | 역할                  |
|------------------------|---------------|---------------------|
| `users`                | User          | 사용자 계정과 OIDC 식별자 저장 |
| `friend_relationships` | Friends       | 수락된 친구 관계 저장        |
| `friend_request`       | Friends       | 대기 중인 친구 요청 저장      |
| `friend_restrictions`  | Friends       | 차단/거절 제한 저장         |
| `fcm_token`            | Notifications | FCM 디바이스 토큰 저장      |
| `notification_history` | Notifications | 발송 이력 저장            |
| `terms`                | Terms         | 약관 버전과 본문 저장        |
| `user_agreement`       | Terms         | 사용자별 약관 동의 이력 저장    |

### 관계 요약

- `users`는 모든 도메인의 기준 엔티티다.
- `friend_relationships`는 수락 시 양방향 행을 저장한다.
- `friend_request`는 요청자/수신자를 구분해 보관한다.
- `friend_restrictions`는 `REJECT`, `BLOCK` 타입으로 제한 상태를 저장한다.
- `terms`는 약관 타입과 버전을 관리한다.
- `notification_history`는 알림 제목, 본문, 타입, path, 읽음 여부를 저장한다.

---

## 인증과 보안

### OIDC

- Kakao와 Google 모두 `nonce`를 사용한다.
- 로그인 및 회원가입 요청에서 `nonce`를 토큰의 `nonce` 클레임과 대조한다.
- Google issuer는 Google 계정 발급자 형식을 허용한다.

### JWT

- Access Token과 Refresh Token을 분리한다.
- Refresh Token은 앱 메모리 Caffeine 기반으로 관리한다.

### 어드민

- 어드민 로그인은 OTT(One-Time Token) 흐름을 사용한다.
- 어드민 API와 뷰가 공존할 때는 세션과 JWT를 함께 고려한다.

### 역할

- `ROLE_NORMAL`: 일반 사용자
- `ROLE_ADMIN`: 관리자

### 보안 정책 핵심

- JWT Secret과 OTT Webhook은 환경변수로 관리한다.
- 공개 경로와 테스트 필터 체인을 일치시킨다.
- 인증/인가 실패는 전역 예외 규칙을 따른다.

---

## API 규칙

### 응답 형식

- 모든 응답은 `ApiResponse<T>` 하나로 통일된다: `imhereResponseCode`, `message`, `data`.
- 성공은 `imhereResponseCode = "SUCCESS"`, 실패는 도메인별 에러 코드 문자열이다.

### 에러 코드 체계

- `GLOBAL-*`
- `AUTH-*` / `TOKEN-*` (인증/토큰 분리)
- `USER-*`
- `FRIEND-*`
- `TERM-*`
- `SMS-*`
- `FCM-*`

엔드포인트별 실제 코드 예시는 자동 생성 API 문서(`src/main/resources/static/docs/openapi3.yaml`, RestDocs 기반)에 들어 있다.

### 예외 처리

- 도메인 예외는 enum과 `.throwIt()` 흐름을 사용한다.
- 컨트롤러에서 try-catch를 남용하지 않는다.
- 전역 예외 핸들러가 응답과 로그를 함께 정리한다.

---

## 알림 시스템

### 발송 구조

- HTTP 요청에서 바로 외부 발송하지 않는다.
- RabbitMQ 큐에 먼저 적재한다.
- Consumer가 비동기로 처리한다.
- 동일 요청 중복 처리 방지를 위해 멱등성을 유지한다.

### 채널

- FCM: 앱 푸시 알림
- SMS: 문자 알림

### FCM 라우팅

- 알림 클릭 후 앱 내부 화면 이동은 `extraData.path`로 전달한다.
- `path`는 `/`로 시작해야 한다.
- 페이지 이름이 아니라 실제 경로 문자열을 사용한다.

### 알림 이력

- `notification_history`에 수신자, 발신자, 제목, 본문, 타입, 경로, 읽음 여부를 저장한다.
- 시스템 알림의 발신자 표시는 `ImHere`를 사용한다.

---

## 관측성과 로깅

- 로그는 구조화해서 남긴다.
- 민감 정보는 마스킹한다.
- traceId로 요청 흐름을 추적한다.
- 앱 로그, 메트릭, 트레이스는 Grafana Alloy를 통해 수집한다.
- Loki, Prometheus, Tempo를 Grafana Cloud로 보낸다.

---

## 인프라

### 현재 구성

- App Server
    - Spring Boot
    - Nginx
    - Grafana Alloy
- Database Server
    - MySQL
- Middleware Server
    - Caffeine
    - RabbitMQ

### Compose 파일

- `docker-compose.yml`: 단일 원본, `local` / `infra` / `prod` profile로 분기

### 배포 관련 파일

- `Dockerfile.release`
- `infra/nginx/nginx.conf.template`
- `infra/nginx/nginx.conf`
- `infra/alloy/alloy-config.alloy.template`
- `infra/alloy/alloy-config.alloy`
- `infra/nginx/website.html`
- runtime `prod.env` (config repo)
- `infra/scripts/sync-config.sh`
- `secrets/`

---

## 환경 설정

### 프로파일

- `application.yaml`
- 로컬 기본값은 `application-local.yaml`에서 읽는다(`spring.profiles.default=local`).

### 런타임 설정

- `prod.env`
- `application-local.yaml`
- `secrets/imhereFirebaseKey.json`

### config repo

- `C:\Project\ImHere\config`
- `prod.env`
- `imhereFirebaseKey.json`

### 운영 변수

- DB 접속 정보
- Caffeine 설정
- RabbitMQ 접속 정보
- Grafana Cloud 자격증명
- Firebase 키 경로
- OIDC 관련 설정

### 운영용 compose 변수 예시

- `CONFIG_REPO_PAT`
- `prod.env`에 들어가는 DB/RabbitMQ/Grafana Cloud 값들

### 로컬에서 설정이 들어가는 방식

- `./gradlew bootRun`은 `application.yaml` + `application-local.yaml` 조합으로 뜬다.
- `docker compose --profile local --profile infra up -d`는 `docker-compose.yml`에 적힌 기본값으로 뜬다.
- config repo의 `prod.env` / `imhereFirebaseKey.json`은 운영 배포 전용이다.

---

## 로컬 실행

### 필요 환경

- JDK
- Docker
- Docker Compose

### 로컬 인프라

```bash
docker compose --profile local --profile infra up -d
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

---

## 테스트 전략

### 단계

- Unit Test: 도메인, 독립 서비스
- Slice Test: Controller, Repository
- Integration Test: 실제 DB/RabbitMQ 포함 E2E

### 규칙

- Controller는 슬라이스 테스트로 검증한다.
- 실제 인증/인가와 비즈니스 에러는 통합 테스트로 검증한다.
- 새로운 API 엔드포인트는 Integration Test와 RestDocs를 함께 작성한다.
- Testcontainers 사용 시 Docker 데몬이 필요하다.

---

## 운영 절차

- 초기 배포는 필요한 설정 파일을 EC2에 반영한 뒤 수행한다.
- 인증서 갱신은 호스트의 Certbot과 Nginx 조합을 사용한다.
- 장애 시 로그와 traceId로 원인을 추적한다.
- 알림 실패는 RabbitMQ/DLQ 흐름과 외부 서비스 상태를 함께 본다.

---

## 문서 인덱스

상세 설계/운영 문서는 `docs/`에 있다.

| 문서                                                 | 내용                                       |
|----------------------------------------------------|------------------------------------------|
| [docs/architecture.md](./docs/architecture.md)     | 전체 시스템 토폴로지, 배포 구조, 외부 의존성               |
| [docs/domain.md](./docs/domain.md)                 | Auth/Friends/Notifications/Terms 비즈니스 규칙 |
| [docs/security.md](./docs/security.md)             | OIDC/JWT/Admin OTT 인증 정책                 |
| [docs/error-handling.md](./docs/error-handling.md) | 응답 포맷, 도메인 에러 코드 패턴                      |
| [docs/api-spec.md](./docs/api-spec.md)             | 엔드포인트 그룹, 자동생성 API 문서 위치                 |
| [docs/db-schema.md](docs/infra/db-schema.md)       | DDL, ERD, 테이블별 참고사항                      |
| [docs/flows.md](./docs/flows.md)                   | 주요 시퀀스 다이어그램(로그인/가입/친구/알림/DLQ)           |
| [docs/deployment.md](docs/infra/README.md)         | Docker, CI/CD, AWS, 도메인/DB 호스팅           |
| [docs/observability/README.md](./docs/observability/README.md)   | 로그/메트릭/트레이스 파이프라인, 알림 채널                 |
| [docs/test-guideline.md](./docs/test-guideline.md) | 테스트 네이밍/전략/도구                            |

모바일 클라이언트 저장소: <https://github.com/kdongsu5509/imhere_mobile>
