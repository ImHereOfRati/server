---
name: observability-rules
description: 커스텀 Access Log, 민감 데이터 마스킹(BodyMasker), Micrometer Tracing(Trace ID) 등을 활용한 로깅 및 관측성 규칙을 정의한다.
---

# Observability & Logging Rules

## Rule 1 — Use Custom Logging Components

이 프로젝트는 `support/logger` 패키지에 `LoggingFilter`, `AccessLogPrinter`, `BodyMasker` 등의 커스텀 로깅 메커니즘이 이미 갖춰져 있습니다.
따라서 컨트롤러 내에서 HTTP Request나 Response Payload를 개별적으로 `log.info`로 찍을 필요가 없습니다. 필터 단에서 자동으로 마스킹 및 포맷팅되어 남습니다.

## Rule 2 — Data Masking

비밀번호, 토큰 등 민감한 개인정보(PII)가 포함된 DTO나 엔티티를 부득이하게 로깅할 때는 원문 그대로 남기지 않습니다.
프로젝트의 `BodyMasker` 로직을 활용하거나 별도의 마스킹 처리를 통해 로그 유출 사고를 방지합니다.

## Rule 3 — Meaningful Business Logging

에러 로그를 남기거나 예외를 발생시킬 때는 단순히 "조회 실패"와 같은 메시지만 남기지 마세요. 
어떤 식별자(ID)를 사용하여 어떤 상태에서 실패했는지 알 수 있도록 **Context Data**를 포함해야 합니다.

```kotlin
// ✅ 권장 (컨텍스트 데이터 포함)
UserException.USER_NOT_ACTIVE.throwIt(
    contextData = mapOf("userId" to userId, "status" to currentStatus)
)
```

## Rule 4 — Distributed Tracing (MDC)

`build.gradle`에 `Micrometer Tracing (OTLP)`이 설정되어 있어 로그마다 고유한 Trace ID가 자동으로 남습니다.
비동기 처리(`@Async` 또는 RabbitMQ 메시지 큐 발행)를 작성할 때는 부모 스레드의 Trace ID 컨텍스트가 유실되지 않도록 설정(MDC 전파)에 유의하여 끊김 없는 추적이 가능하도록 해야 합니다.
