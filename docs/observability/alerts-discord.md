# Alerts / Discord

Discord는 Grafana Cloud와 다른 역할을 가진다.

- Grafana Cloud: 로그, 메트릭, 트레이스 수집과 조회
- Discord: 운영 이벤트를 즉시 알리는 채널

즉 Discord는 "관측 데이터 저장소"가 아니라 "운영 알림 전달 채널"이다.

## Webhook 종류

현재 코드와 설정 기준으로 webhook은 세 종류다. 각 webhook은 서로 다른 클래스가 읽으며, 발화 조건도 다르다.

| Webhook | 발화 조건 | 보내는 클래스 | application.yaml 키 | prod.env 키 |
|---|---|---|---|---|
| server error | 응답 status >= 500 | `AccessLogPrinter` | `discord.url.error.server` | `DISCORD_WEBHOOK_ERROR_SERVER` |
| client error | 4xx 비즈니스 예외 / 403 비정상 접근 | `DiscordUserErrorNotifier` | `discord.url.error.client` | `DISCORD_WEBHOOK_ERROR_CLIENT` |
| ott | 관리자 OTT 발급 성공 | `ImHereOttSuccessHandler` | `discord.url.ott` | `DISCORD_WEBHOOK_OTT` |

세 키 모두 `application.yaml`의 `discord.url.*`에서 읽고(`application.yaml:150-155`), 운영에서는 `prod.env`로 주입된다.

주의할 점은 server webhook(5xx)과 client webhook(4xx)이 **서로 다른 코드 경로**에서 나간다는 것이다. 5xx는 컨트롤러 advice가 아니라 access 로그 필터 경로(`AccessLogPrinter`)에서 잡고, 4xx는 `@RestControllerAdvice` 핸들러에서 잡는다. 이 둘을 한 핸들러로 합치지 않은 이유는 [코드 경로](#코드-경로)에서 설명한다.

## 코드 경로

전송 자체는 `DiscordMessageSender` 한 곳으로 모이고, "무엇을 언제 보낼지"는 호출하는 쪽이 정한다.

### `DiscordMessageSender` (전송 계층)

- 실제 webhook HTTP 호출을 담당하는 low-level sender다(`DiscordApiClient`를 호출).
- `DiscordMessageSendPort` 인터페이스를 구현한다. `AccessLogPrinter`, `DiscordUserErrorNotifier`는 이 포트(인터페이스)에 의존하고, `ImHereOttSuccessHandler`는 구현체(`DiscordMessageSender`)에 직접 의존한다.
- `sendMessage`는 `@Async("discordExecutor")`다(`DiscordMessageSender.kt:15`). 알림 발송이 요청 처리 스레드를 막지 않게 별도 executor에서 fire-and-forget으로 보낸다. 트레이드오프: 전송 실패는 재시도하지 않고 `logger.error`로만 남긴다(`DiscordMessageSender.kt:25-27`). 알림은 best-effort 신호일 뿐 유실되어도 본 요청 처리에는 영향이 없어야 한다는 판단이다 — 영구 보존이 필요한 데이터는 Grafana Cloud Loki 쪽에 이미 남는다.
- webhook URL이 비어 있으면 전송을 건너뛴다(`DiscordMessageSender.kt:30-36`). 로컬/미설정 환경에서 알림만 빠지고 앱은 정상 동작하게 하기 위한 가드다.

### `AccessLogPrinter` (5xx → server webhook)

- access 로그를 출력하면서, 응답 status가 500 이상이면 server webhook으로 알림을 보낸다(`AccessLogPrinter.kt:26-32`).
- `discord.url.error.server`를 읽는 **유일한** 클래스다(`AccessLogPrinter.kt:15`).
- 알림 본문에는 traceId, method, URI, 응답 시간, IP와 포맷된 access 로그가 들어간다(`build5xxAlert`, `AccessLogPrinter.kt:34-47`). traceId를 함께 보내므로 Discord 알림에서 Grafana Cloud의 같은 요청 로그/트레이스로 바로 추적할 수 있다.
- 5xx를 컨트롤러 advice가 아니라 로그 필터 경로에서 잡는 이유: `GlobalExceptionHandler`가 잡지 못하고 빠져나간 예외나 필터/서블릿 단계 오류도 결국 status 500으로 응답에 찍히므로, 응답 status를 보는 이 지점이 5xx를 가장 빠짐없이 포착한다.

### `DiscordUserErrorNotifier` (4xx → client webhook)

- 운영에서 알릴 가치가 있는 클라이언트 오류를 분류해 client webhook으로 보낸다. `discord.url.error.client`만 읽는다(`DiscordUserErrorNotifier.kt:10`).
- 메서드는 두 개다: `notifyUserError`(4xx 비즈니스/입력 오류)와 `notifyAbnormalAccess`(403 비정상 접근).
- `GlobalExceptionHandler`, `SecurityExceptionHandler`가 이 클래스를 사용한다.

### `GlobalExceptionHandler` (4xx 비즈니스 예외 진입점)

- 컨트롤러 레벨 예외를 공통 응답으로 바꾸는 `@RestControllerAdvice` 진입점이다.
- `ImHereBaseException`(프로젝트 공통 비즈니스 예외)을 처리할 때만 `notifyUserError`로 client webhook 알림을 보낸다(`GlobalExceptionHandler.kt:42-46`).
- **500 핸들러(`handleException`, `GlobalExceptionHandler.kt:189-198`)는 Discord 알림을 보내지 않는다.** 5xx 알림은 위 `AccessLogPrinter`가 담당한다. (이전 문서는 GlobalExceptionHandler가 서버 예외를 Discord로 연결한다고 적었으나 코드와 다르다.)

### `SecurityExceptionHandler` (403 비정상 접근 진입점)

- Spring Security 계층의 `AuthorizationDeniedException`을 다루는 `@RestControllerAdvice(@Order(3))`다.
- 권한 거부 시 `notifyAbnormalAccess`로 client webhook에 알림을 보낸다(`SecurityExceptionHandler.kt:28-32`).

### `ImHereOttSuccessHandler` (OTT → ott webhook)

- 관리자 OTT 발급 성공 시점을 다루는 `OneTimeTokenGenerationSuccessHandler`다.
- 발급 성공 시 `discord.url.ott` webhook으로 발급 정보(관리자, IP, 시각, 토큰)를 보낸다(`ImHereOttSuccessHandler.kt:49-50`).
- 같은 사용자가 5분 내 3회 이상 요청하면 429로 막는 in-memory rate limit이 붙어 있다(`canIssueOtt`, `ImHereOttSuccessHandler.kt:54-74`). 이 이벤트는 일반 오류 알림과 분리되어 별도 채널(`DISCORD_WEBHOOK_OTT`)로 간다.

## 운영 관점에서 중요한 점

- Discord는 로그 전체를 복제하지 않는다.
- Discord는 "즉시 알아야 하는 운영 이벤트"만 보낸다.
- 따라서 Grafana Cloud가 장기 조회 경로라면, Discord는 실시간 신호 경로에 가깝다.

## 보안 주의

- webhook URL은 사실상 bearer secret처럼 취급해야 한다.
- URL이 노출되면 별도 인증 없이 해당 채널로 메시지를 보낼 수 있다.
- 그래서 운영에서는 `prod.env` 또는 배포 비밀 주입 경로에서만 관리한다.

## 관련 문서

- [Overview](./README.md)
- [Signals](./signals.md)
- [Runtime Config](./runtime-config.md)
