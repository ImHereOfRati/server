# Architecture

ImHere 서버의 현재 운영 구성을 기준으로, 요청이 어떤 경로로 들어와 어떤 외부 의존성을 거쳐 처리되는지 정리한 문서다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 알림은 커밋 후 비동기 파이프라인 | 사용자 요청은 Spring MVC, 푸시는 Modulith Application Event listener가 담당한다 | FCM 실패나 재시도 정책을 HTTP 요청 수명주기와 분리한다 |
| 관측 데이터 업로드는 Alloy 집중 | 앱 컨테이너는 OTLP 로컬 수집까지만 담당하고 Grafana Cloud 전송은 Alloy 가 맡는다 | 애플리케이션 설정과 벤더 종속 전송 설정을 분리한다 |
| 세션보다 토큰 중심 인증 | 앱 API 는 JWT, 관리자 콘솔은 OTT 이후 세션 로그인으로 분기한다 | 모바일 API 와 관리자 웹의 접근 형태가 다르기 때문이다 |

---

## 시스템 맵

```text
[Flutter App]
    |
    | HTTPS :443
    v
[nginx on app EC2]
    |
    | HTTP :8080
    v
[Spring Boot app: dsko]
    |-- MySQL (Gabia hosted)
    |-- Caffeine cache (JWKS / refresh token)
    |-- Spring Modulith Event Publication Registry
    |-- Firebase FCM
    |-- Kakao / Google OIDC
    |-- Solapi
    `-- Discord Webhook

[alloy on app EC2] -> Grafana Cloud
```

---

## 요청 경로

1. 클라이언트는 `https://fortuneki.site` 로 요청한다.
2. nginx 가 TLS 종료와 기본 CORS 처리를 맡고 `dsko:8080` 으로 프록시한다.
3. Spring Boot 는 JWT 필터 또는 Admin 보안 체인을 통해 인증을 처리한다.
4. 영속 상태는 MySQL 에 저장하고, 짧은 수명 상태는 로컬 캐시에 둔다.
5. 알림성 작업은 업무 트랜잭션과 함께 이벤트로 기록하고 커밋 후 listener가 비동기로 FCM/SMS 전송을 수행한다.
6. 로그/메트릭/트레이스는 로컬 OTLP 수집 후 Alloy 가 Grafana Cloud 로 밀어 올린다.

---

## 런타임 구성

| 레이어 | 구성 요소 | 역할 |
|---|---|---|
| Edge | `nginx` | TLS 종료, 리버스 프록시, CORS |
| App | `dsko` | 인증, 친구, 약관, 알림 API |
| Events | Spring Modulith | 트랜잭션 이벤트 기록과 커밋 후 비동기 전달 |
| Data | Gabia MySQL | 사용자, 친구, 토큰, 알림 생애주기 영속화 |
| Telemetry | `alloy` | OTLP 수집 후 Grafana Cloud 전송 |

---

## 외부 의존성

| 의존성 | 용도 | 장애 시 영향 |
|---|---|---|
| Kakao / Google OIDC | 로그인용 ID Token 검증 | 신규 로그인 불가 |
| Firebase FCM | 푸시 전송 | 알림이 FAILED/DEAD로 전이 |
| Gabia MySQL | 핵심 도메인 데이터 저장 | 대부분의 API 영향 |
| Discord Webhook | 관리자 OTT, 운영 알림 | 운영자 통지 지연 |

---

## 코드 기준점

- `src/main/kotlin/com/kdongsu5509/auth/security/config/SecurityConfig.kt`
- `src/main/kotlin/com/kdongsu5509/notifications/adapter/in/event/`
- `src/main/kotlin/com/kdongsu5509/notifications/application/service/NotificationDeliveryService.kt`
- `src/main/resources/application.yaml`

---

## 연관 문서

- [domain.md](domain.md)
- [internal-architecture.md](internal-architecture.md)
- [../security/README.md](../security/README.md)
- [../flows/README.md](../flows/README.md)
- [../infra/README.md](../infra/README.md)
