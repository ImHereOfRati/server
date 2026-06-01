---
name: async-external-rules
description: 외부 API 연동(Port/Adapter) 및 비동기 처리(RabbitMQ, @Async) 시 준수해야 할 규칙을 정의한다.
---

# Async & External Integration Rules

## Rule 1 — Port and Adapter for External Calls

Discord, Firebase(FCM), SMS API 등 외부 시스템을 연동할 때는 **핵심 비즈니스 로직(Service)이 외부 라이브러리/API 클래스에 직접 의존하지 않도록** 헥사고날 아키텍처의 Port(인터페이스)를 도입해야 합니다.

* 도메인의 `application/port/out` 패키지(또는 하위 패키지)에 인터페이스(Port) 정의
* `support/external` 또는 해당 도메인의 인프라 패키지에 실제 구현체(Adapter) 작성

```kotlin
// ✅ Service는 Port 인터페이스에만 의존하여 외부 기술이 바뀌어도 비즈니스 로직은 보호됨
@Service
class NotificationService(private val externalMessagePort: ExternalMessagePort)
```

## Rule 2 — Asynchronous Processing

사용자 경험(응답 시간)을 저해하는 무거운 연산이나 외부 시스템 전송(예: 푸시 알림, 대량 이메일 발송)은 HTTP 요청 쓰레드에서 **동기적으로 처리하지 않습니다**.

* 프로젝트 내에 구축된 **RabbitMQ**로 이벤트를 발행하여 컨슈머 워커가 비동기로 안전하게 처리하도록 위임합니다.
* 또는 설정된 `AsyncConfig`의 스레드풀을 활용해 `@Async` 어노테이션으로 비동기 처리를 수행해야 합니다.
