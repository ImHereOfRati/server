# ImHere Server

ImHere의 서버 애플리케이션입니다.

사용자의 도착/출발 알림 요청을 처리하며,
인증, 문자 발송, 푸시 알림 등의 기능을 제공합니다.

> 홈페이지 : https://imhere.ratiko.co.kr

> 모바일 레포지토리 : https://github.com/ImHereOfRati/mobile

> 플레이 스토어 : https://play.google.com/store/apps/details?id=com.kdongsu5509.iamhere

---

<p>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="Gradle"/>
</p>
<p>
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
  <img src="https://img.shields.io/badge/Caffeine-FF9F1C?style=for-the-badge&logo=coffeescript&logoColor=white" alt="Caffeine"/>
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
- Kakao/Google/Apple OIDC 로그인
- JWT 발급 및 Refresh Token 재발급
- 사용자, 친구, 약관, 알림, 기록, 운영 도메인 분리
- Spring Modulith 이벤트 기반 비동기 알림 처리
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
| Events        | Spring Modulith Application Events                 |
| Auth          | Kakao/Google/Apple OIDC, JWT, Spring Security      |
| Admin Auth    | Spring Security OTT                                |
| Push          | Firebase Admin SDK (FCM)                           |
| SMS           | Solapi SDK                                         |
| Alerting      | Discord Webhook                                    |
| API Docs      | Spring REST Docs, OpenAPI 3                        |
| Observability | Micrometer, Prometheus, Grafana Alloy, Loki, Tempo |
| Test          | JUnit 5, Mockito, AssertJ, MockMvc                  |
| Coverage      | JaCoCo                                             |
| Infra         | AWS EC2, ECR, Docker                               |

---

## 서비스 도메인

### 인증

- Kakao OIDC 로그인
- Google OIDC 로그인
- Apple OIDC 로그인
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
- Spring Modulith 도메인 이벤트
- DB UNIQUE 멱등성 및 DEAD 상태 재시도

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

| 테이블                 | 도메인           | 역할                                       |
|---------------------|---------------|------------------------------------------|
| `users`             | User          | 사용자 계정과 OIDC 식별자 저장                      |
| `friend_relations`  | Friends       | 두 사람 사이의 관계 한 행. 요청·친구·거절·차단을 상태로 구분해 저장 |
| `fcm_token`         | Notifications | FCM 디바이스 토큰 저장                           |
| `notification`      | Notifications | 발송 생애주기와 수신함 저장                          |
| `terms`             | Terms         | 약관 버전과 본문 저장                             |
| `user_agreement`    | Agreement     | 사용자별 약관 동의 이력 저장                         |
| `one_time_tokens`   | Auth          | 어드민 OTT 로그인용 일회성 토큰 저장 (Spring Security) |
| `event_publication` | (프레임워크)       | Spring Modulith가 모듈 간 이벤트 처리 상태를 기록      |

### 스키마 변경

이 프로젝트에는 Flyway나 Liquibase 같은 마이그레이션 도구가 없다. JPA는 `ddl-auto: validate`로
띄우므로 스키마를 자동으로 바꾸지 않는다. 스키마의 현재 모습은
`db/init/mysql/imhere-full-init.sql` **한 파일**이 전부 들고 있고, 항상 최신 상태로 유지한다.

이 파일은 매번 전체 스키마를 새로 만든다(`DROP TABLE IF EXISTS` 후 `CREATE TABLE`). 즉
**적용하면 기존 데이터가 사라진다.** 로컬·테스트처럼 언제든 다시 만들어도 되는 DB를 전제로 한다.

`ENUM` 값 추가는 놓치기 쉽다. `validate`는 컬럼 타입만 보고 `ENUM` 안의 값 목록까지는
검사하지 않기 때문에, 스키마를 안 고쳐도 애플리케이션은 멀쩡히 뜬다. 그러다 새 값으로
처음 INSERT가 일어나는 순간 데이터 잘림 오류가 난다. Kotlin enum에 값을 더할 때는
이 SQL도 함께 고친다.

```bash
mysql -h "$DB_HOST" -u "$DB_USER" -p "$DB_NAME" < db/init/mysql/imhere-full-init.sql
```

### 관계 요약

- `users`는 모든 도메인의 기준 엔티티다.
- `friend_relations`는 두 사람의 관계를 한 행으로 저장한다. 예전에는 요청·친구·제한을 세 테이블로
  나눴지만, 셋은 같은 관계의 다른 시점이라 상태 전이로 다루는 편이 맞았다.
  요청을 수락하면 새 행이 생기는 게 아니라 그 행의 `status`가 `REQUESTED`에서 `ACCEPTED`로 바뀐다.
- 관계에는 방향이 없다. 두 사람의 식별자를 정렬해 `low_user_id` / `high_user_id`에 넣고,
  `uk_friend_pair`로 같은 쌍이 두 행이 되는 것을 막는다. "누가 걸었는지"는 `initiated_user_id`가
  따로 들고 있어서, 거절처럼 주체가 뒤집히는 전이도 행을 옮기지 않고 표현된다.
- `low_alias` / `high_alias`는 각 자리의 주인이 상대를 부르는 이름이다. 별칭은 자기 칸만 바꾼다.
- `expired_at`은 제한이 풀리는 시각이다. 거절은 한 달 뒤, 차단은 사실상 만료되지 않는다.
- `terms`는 약관 타입과 버전을 관리한다.
- `notification`은 알림 내용, 전달 수단, 발송 상태, 재시도와 읽음 여부를 함께 저장한다.

---

## 인증과 보안

### OIDC

- Kakao, Google, Apple 모두 `nonce`를 사용한다.
- 로그인 및 회원가입 요청에서 `nonce`를 토큰의 `nonce` 클레임과 대조한다.
- 허용 `iss`와 `aud`는 provider마다 목록으로 설정한다. Google은 `iss`를 스킴 포함/미포함 두 형태로
  발급하고, Google과 Apple은 플랫폼마다 client ID(`aud`)가 갈리기 때문이다.
- Apple ID Token에는 표시 이름이 없어 닉네임은 이메일 앞부분을 쓴다. 이메일 자체가 없으면
  `users.email`이 필수라 로그인을 거절한다.
- 자세한 내용은 [docs/security/oauth.md](docs/security/oauth.md)에 있다.

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
- 발행 트랜잭션과 함께 Modulith Event Publication Registry에 기록한다.
- 커밋 후 이벤트 리스너가 비동기로 처리한다.
- `dedupe_key` UNIQUE 제약으로 동일 요청의 중복 발송을 억제한다.
- 발송 직전에 알림 종류를 제목·본문·푸시 정책으로 변환한다. 이때 발신자 표시 이름도 함께 정한다
  (친구 별칭 조회 → 없으면 닉네임).

### 채널

- FCM: 앱 푸시 알림
- SMS: 문자 알림

### FCM 라우팅

**서버는 앱 내부 경로를 만들지 않는다.** 알림을 눌렀을 때 어느 화면으로 갈지는 클라이언트가
`data.type`을 보고 정한다.

예전에는 서버가 `/friend/requests` 같은 경로 문자열을 `path`로 내려보냈다. 그러면 앱의 화면
구조가 바뀔 때마다 서버를 배포해야 했다. 경로는 앱의 관심사이므로 앱이 갖는 편이 맞다.

- `data.path` 키는 더 이상 내려가지 않는다.
- 클라이언트가 `type` → 화면 대응표를 갖는다.
- 화면 이동에 추가 값이 필요하면(예: 약관 상세의 `termId`) 그 값은 `data`에 그대로 들어 있다.

### 알림 이력

- `notification`에 수신자, 발신자 표시 이름, 내용, 발송 상태, 시도 횟수, 실패 사유와 읽음 여부를 저장한다.
- 발신자 표시 이름(`sender_alias`)은 **수신자가 발송자를 부르는 이름**이다. 친구 별칭이 있으면
  별칭을, 없으면 발송자 닉네임을 담는다.
- 친구 요청 관련 알림(`FRIEND_REQUEST_RECEIVED`, `FRIEND_REQUEST_ACCEPTED`)은 아직 별칭이 있을 수
  없는 시점이므로 항상 닉네임을 쓴다.
- 시스템 알림의 발신자 표시는 `ImHere`를 사용한다.

### 알림 종류

`NotificationType`은 이름만 갖는다. 제목·본문·푸시 정책은 서버가 발송 직전에 이 이름으로부터
만들어 내고, 화면 이동은 클라이언트가 이 이름으로 정한다.

| type                       | 제목        | 클라이언트 발송 | 필수 `data` 키    |
|----------------------------|-----------|----------|----------------|
| `FRIEND_REQUEST_RECEIVED`  | 새로운 친구 요청 | ✗        | -              |
| `FRIEND_REQUEST_ACCEPTED`  | 친구 요청 수락  | ✗        | -              |
| `LOCATION_TARGET`          | 위치 공유 대상자 알림 | ✓    | `placeName`    |
| `ARRIVAL`                  | 도착 안내     | ✓        | `placeName`    |
| `DEPARTURE`                | 출발 안내     | ✓        | `placeName`    |
| `TERMS_UPDATE_NOTICE`      | 서비스 공지사항  | ✗        | -              |
| `DELIVERY_RESULT_NOTICE`   | 발송 결과 알림  | ✗        | -              |
| `DELIVERY_FAILED_NOTICE`   | 발송 실패 알림  | ✗        | -              |

- **클라이언트 발송**이 ✗인 종류를 `POST /api/notifications`로 보내면 400이 떨어진다.
  서버 내부 이벤트로만 발행되는 알림이다.
- `placeName`이 필요한 종류에 값을 빼먹으면 본문을 만들 수 없어 요청이 거절된다.
- 이 이름들은 FCM `data.type`으로 그대로 직렬화된다. 이름이 곧 와이어 계약이다.

### FCM data 페이로드

```json
{
  "type": "ARRIVAL",
  "senderAlias": "길동이",
  "placeName": "학교"
}
```

`type`과 `senderAlias`는 항상 들어간다. 나머지 키는 발송 요청의 `extraData`가 그대로 실려 온 것이다.

### 클라이언트 영향 (Breaking)

이번 변경으로 앱이 읽던 필드가 바뀌었다. 서버 배포 전에 앱을 함께 맞춰야 한다.

| 위치                             | 변경                                                   |
|--------------------------------|------------------------------------------------------|
| FCM `data`                     | `senderNickname` → `senderAlias`                     |
| FCM `data`                     | `path` 제거 — `type`으로 화면을 정한다                          |
| FCM `data`                     | `senderEmail` 제거                                     |
| 수신함 조회 응답                      | `senderNickname` → `senderAlias`, `path` 제거          |
| 어드민 실패 알림 응답                   | `senderEmail` 제거, `senderNickname` → `senderAlias`   |
| `NotificationType`             | `ARRIVAL_CONFIRMATION` 삭제 — 전송 시 400                  |

`ARRIVAL_CONFIRMATION`은 `ARRIVAL`과 제목·본문·푸시 정책이 완전히 같은 중복 상수였다.
쓰고 있었다면 `ARRIVAL`로 바꾸면 된다.

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
- In-process middleware
    - Caffeine
    - Spring Modulith Event Publication Registry

### Compose 파일

- `docker-compose.yml`: 단일 원본, `local` / `prod` profile로 분기

### 배포 관련 파일

- `Dockerfile.release`
- `infra/nginx/nginx.conf.template`
- `infra/nginx/nginx.conf`
- `infra/alloy/alloy-config.alloy.template`
- `infra/alloy/alloy-config.alloy`
- runtime `env/*.env` (config repo)
- `infra/scripts/sync-config.sh`
- `infra/scripts/remote-provision.sh` / `remote-tls.sh` / `remote-rollout.sh` / `remote-healthcheck.sh`
- `secrets/`

---

## 환경 설정

### 프로파일

- `application.yaml`
- 로컬 기본값은 `application.yaml`의 `${VAR:기본값}`에 직접 들어 있다. 프로파일을
  지정하지 않아도 그대로 뜬다.
- 운영은 `SPRING_PROFILES_ACTIVE=prod`로 같은 파일의 prod 문서를 켠다. 그 문서는
  누출되면 곤란한 변수(`JWT_SECRET`, `MGMT_BASE_PATH` 등)를 기본값 없이 다시
  선언하므로, 값이 빠지면 운영은 로컬 더미로 뜨지 않고 기동에 실패한다.

### 런타임 설정

- `env/app.env`, `env/web.env`, `env/oidc.env`, `env/external.env`, `env/observability.env`
  (관심사별로 쪼개져 있고 컨테이너마다 필요한 것만 주입된다 — private config repo의 `env/`)
- `secrets/imhereFirebaseKey.json`

### config repo

- `ImHereOfRati/config`
- `env/*.env`
- `imhereFirebaseKey.json`

### 운영 변수

- DB 접속 정보
- Caffeine 설정
- Grafana Cloud 자격증명
- Firebase 키 경로
- OIDC 관련 설정

### 운영용 compose 변수 예시

- `CONFIG_REPO_PAT`
- `env/app.env`의 DB 값, `env/observability.env`의 Grafana Cloud 값들

### 로컬에서 설정이 들어가는 방식

- `./gradlew bootRun`은 `application.yaml`의 기본값만으로 뜬다. 프로파일 인자가 필요 없다.
  DB만 `localhost:3306/rati`에 떠 있으면 된다.
- `docker compose --profile local up -d`는 `docker-compose.yml`에 적힌 기본값으로 뜬다.
- config repo의 `env/*.env` / `imhereFirebaseKey.json`은 운영 배포 전용이다.

---

## 로컬 실행

### 필요 환경

- JDK
- Docker
- Docker Compose

### 로컬 인프라

```bash
docker compose --profile local up -d
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
- Integration Test: 실제 DB와 Modulith 이벤트 경계를 포함한 E2E

### 규칙

- Controller는 슬라이스 테스트로 검증한다.
- 실제 인증/인가와 비즈니스 에러는 통합 테스트로 검증한다.
- 새로운 API 엔드포인트는 Integration Test와 RestDocs를 함께 작성한다.

---

## 운영 절차

- 초기 배포는 필요한 설정 파일을 EC2에 반영한 뒤 수행한다.
- 인증서 갱신은 호스트의 Certbot과 Nginx 조합을 사용한다.
- 장애 시 로그와 traceId로 원인을 추적한다.
- 알림 실패는 `Notification.DEAD` 상태와 외부 서비스 상태를 함께 본다.

---

## 문서 인덱스

상세 설계/운영 문서는 `docs/`에 있다.

| 문서                                                             | 내용                                                     |
|----------------------------------------------------------------|--------------------------------------------------------|
| [docs/README.md](./docs/README.md)                             | 문서 전체 인덱스                                               |
| [docs/architecture/](./docs/architecture/README.md)            | 시스템 토폴로지, 모듈 내부 구조, 도메인 비즈니스 규칙                          |
| [docs/security/](./docs/security/README.md)                    | OIDC/JWT/관리자 계정 인증 정책                              |
| [docs/conventions/](./docs/conventions/README.md)              | Kotlin 컨벤션, 에러 응답 포맷, 테스트 전략                            |
| [docs/flows/](./docs/flows/README.md)                          | 주요 시퀀스 다이어그램(로그인/가입/친구/알림/재발송)                          |
| [docs/infra/](./docs/infra/README.md)                          | Docker, CI/CD, AWS, nginx, 가비아 도메인/DB 호스팅, DB 스키마       |
| [docs/observability/](./docs/observability/README.md)          | 로그/메트릭/트레이스 파이프라인, 런타임 설정, 알림 채널                        |

모바일 클라이언트 저장소: <https://github.com/ImHereOfRati/mobile>
