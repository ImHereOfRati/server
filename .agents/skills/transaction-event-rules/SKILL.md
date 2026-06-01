---
name: transaction-event-rules
description: @Transactional(readOnly=true) 성능 최적화 및 도메인 간 결합도를 낮추기 위한 스프링 이벤트(@TransactionalEventListener) 활용 규칙을 정의한다.
---

# Transaction & Event Rules

## Rule 1 — Read-Only Transactions

데이터의 변경(Insert, Update, Delete)이 발생하지 않는 모든 조회 전용 Service 로직에는 반드시 `@Transactional(readOnly = true)`를 선언해야 합니다.
JPA의 더티 체킹(Dirty Checking) 스냅샷 유지를 생략하여 메모리와 성능을 최적화하고, 데이터베이스 Replication 구조에서 Read DB로 요청을 분산시키는 트리거 역할을 합니다.

```kotlin
// ✅ 권장
@Transactional(readOnly = true)
fun findUser(id: Long): User {
    return userRepository.findByIdOrNull(id) ?: UserException.USER_NOT_FOUND.throwIt()
}
```

## Rule 2 — Decoupling with Spring Events

주요 비즈니스 로직(예: 회원가입) 처리 후 부가적인 타 도메인 작업(예: 웰컴 이메일 발송, 가입 알림, 포인트 지급)이 필요한 경우, 
서비스 클래스 간에 직접 의존성을 맺고 메서드를 호출하면 **강결합(Tight Coupling)** 이 발생합니다. 이를 피하기 위해 **Spring ApplicationEventPublisher**를 활용해 이벤트를 발행하세요.

```kotlin
// ❌ 금지 (강결합 및 외부 네트워크 타임아웃이 본 트랜잭션 롤백 유발)
fun registerUser(req: RegisterReq) {
    val user = userRepository.save(User(...))
    notificationService.sendWelcomeMessage(user) 
}

// ✅ 권장 (이벤트 발행으로 결합도 해소)
fun registerUser(req: RegisterReq) {
    val user = userRepository.save(User(...))
    eventPublisher.publishEvent(UserRegisteredEvent(user.id))
}
```

## Rule 3 — @TransactionalEventListener

이벤트를 구독(Subscribe)할 때는 일반 `@EventListener` 대신 **`@TransactionalEventListener`** 를 사용해야 합니다.
본 로직의 트랜잭션이 성공적으로 커밋(`phase = TransactionPhase.AFTER_COMMIT`)된 것을 보장한 후에만 부가 로직이 실행되도록 하여, 본 트랜잭션이 롤백되었음에도 이메일이 발송되는 등의 데이터 정합성 문제를 완벽히 차단합니다.

```kotlin
@Component
class UserNotificationListener {
    
    // 메인 로직 커밋 확인 후 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUserRegistered(event: UserRegisteredEvent) {
        // 비동기 알림 전송 큐 발행 등 부가 로직 수행
    }
}
```
