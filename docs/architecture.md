# Architecture

시스템 전체 구조를 코드/설정 기준으로 정리한다. 도메인 규칙은 [domain.md](./domain.md), 보안은 [security.md](./security.md), 배포는 [deployment.md](./deployment.md)를 본다.

## 시스템 토폴로지

```
[Flutter App]
     │ HTTPS :443
     ▼
[nginx]  ── TLS 종료, CORS, HTTP→HTTPS 리다이렉트
     │ HTTP :8080 (127.0.0.1 루프백만)
     ▼
[Spring Boot (dsko)] ──MySQL(가비아 별도 호스트)
     │        ├──Redis (별도 infra 호스트, OIDC JWKS 캐시 / 알림 dedup / refresh 토큰)
     │        └──RabbitMQ (별도 infra 호스트, 알림 큐+DLQ)
     │
     ├──Firebase FCM (푸시 발송)
     ├──Kakao/Google OIDC (로그인 검증)
     ├──Solapi (SMS 발송)
     └──Discord Webhook (에러/OTT 알림)
     │
     ▼
[alloy] → Grafana Cloud (Loki/Prometheus/Tempo)
```

## 배포 토폴로지 (현재 실제 상태)

- 앱 서버 EC2 1대: `dsko`(Spring Boot) + `nginx` + `alloy` (`docker-compose.prod.yml`)
- 별도 infra 서버: Redis + RabbitMQ (`docker-compose.infra.yml`)
- MySQL: 가비아 별도 호스트
- 배포 자동화: GitHub Actions CI(테스트) → CD(JAR 빌드 → ECR 푸시 → EC2에 SSH로 `docker compose up -d`). 자세한 내용은 [deployment.md](./deployment.md).

## 요청 경로

1. Flutter 앱이 `https://fortuneki.site`로 요청한다.
2. nginx가 TLS를 종료하고 CORS preflight를 즉시 처리한 뒤 `http://dsko:8080`으로 프록시한다.
3. Spring Boot가 JWT(또는 `/api/auth/**`는 무인증)로 인증/인가 후 비즈니스 로직을 수행한다.
4. 알림이 필요한 작업은 RabbitMQ(`imhere.noti.topic`)에 메시지를 발행하고, Consumer가 비동기로 FCM/SMS를 호출한다.

## 핵심 설계 특징

- **위치 판정은 클라이언트 책임이다.** 서버 코드에는 geofence/위치 도메인이 없다 — 지오펜스 진입/이탈 판정은 Flutter 앱이 OS 위치 서비스로 직접 수행하고, 서버는 "누구에게 어떤 알림을 보낼지"만 책임진다(RabbitMQ routing key `noti.service.location.*`).
- **알림은 항상 비동기 + 재시도다.** 컨트롤러가 직접 FCM을 호출하지 않고 RabbitMQ를 거친다 — 실패 시 지수 백오프 재시도(최대 3회) 후 DLQ로 보낸다. 자세한 내용은 [flows.md](./flows.md#알림-발송-흐름).
- **자체 비밀번호 체계가 없다.** Kakao/Google OIDC ID Token 검증만으로 인증한다. 자세한 내용은 [security.md](./security.md).
- **단일 응답 포맷.** 모든 API가 `ApiResponse<T>{imhereResponseCode, message, data}` 하나로 응답한다. 자세한 내용은 [error-handling.md](./error-handling.md).

## 외부 의존성 요약

| 의존성 | 용도 | 장애 시 영향 |
|---|---|---|
| Kakao/Google OIDC | 로그인 검증 | 신규 로그인/가입 불가 (기존 JWT는 영향 없음) |
| Firebase FCM | 푸시 발송 | 알림 발송 실패 → RabbitMQ 재시도 → DLQ 적재, 서비스 자체는 정상 |
| RabbitMQ | 알림 비동기 처리 | 알림 발송 지연/중단, API 자체는 영향 없음 |
| Redis | OIDC 키 캐시·알림 dedup·refresh 토큰 조회 | 캐시 미스로 외부 JWKS 호출 증가, 강제 로그아웃 기능 영향 |
| MySQL(가비아) | 모든 영속 데이터 | 전체 서비스 영향 |
| Discord Webhook | 에러/OTT 알림 통지 | 운영자 알림만 누락, 서비스 자체는 정상 |
