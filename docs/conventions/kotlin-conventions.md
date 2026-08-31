# Kotlin Coding Convention

ImHere 서버의 Kotlin 구현 규칙이다.

이 문서의 모든 규칙은 **현재 저장소 코드에서 확인한 사실**에만 근거한다. 코드와 어긋나는 규칙은 규칙이 아니라 drift로 보고 수정한다. 규칙과 코드가 충돌하면 코드가 우선이며, 코드를 바꿀지 문서를 바꿀지 먼저 결정한다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| null 처리와 예외를 한 줄에서 종료 | `?: <도메인코드>.throwIt()` 사용 (main 19곳) | `OIDCTokenPublicKeyAdapter.kt:22` |
| `!!`는 금지가 아니라 제한 | JPA 생성 식별자 등 저장 이후 non-null이 보장되는 지점에만 허용 | `FriendRelationMapper.kt:14`, `UserResult.kt:21` |
| 설정 주입 두 방식 공존 | 단일 값은 `@param:Value`, 묶음은 `@ConfigurationProperties` | `SecurityConfig.kt:43`, `ImHereJwtProperties.kt:7` |
| 생성자 주입만 사용 | 필드 주입(`@Autowired` 프로퍼티) 사용처 없음 | `GlobalExceptionHandler.kt:26` |
| 조회는 `readOnly`, 쓰기는 메서드 단위 | `readOnly = true` 9곳, 일반 `@Transactional` 21곳 | `NotificationRegister.kt:53` |
| HTTP 밖 벌크 쿼리는 호출 지점에 트랜잭션 | `@Scheduled` + `@Modifying`은 트랜잭션이 자동으로 열리지 않음 | `FriendRestrictionScheduler.kt:14` |
| 테스트는 한국어 `@DisplayName`을 기본으로 | `@DisplayName` 573개, snake_case 함수명 243개 | `AuthAccessDeniedExceptionHandlerTest.kt:27` |

---

## 1. 불변성

* `val`을 기본으로 사용하고 `var`는 필요한 경우에만 쓴다.
* `var`가 남아 있는 대표적인 자리는 `@ConfigurationProperties` 바인딩 클래스다. Spring이 setter로 값을 주입해야 하므로 `var`가 필요하다.

```kotlin
// SecurityWhiteList.kt:5
@ConfigurationProperties(prefix = "security")
class SecurityWhiteList {
    var corsAllowedOrigins: List<String> = emptyList()
}
```

따라서 "`var` 금지"가 아니라 **"프레임워크 바인딩이 요구하지 않는 한 `val`"** 이 실제 규칙이다.

---

## 2. null 처리

### 2.1 기본 패턴

null이면 도메인 예외로 끝낸다. `?:`와 `throwIt()`을 한 줄에서 결합한다.

```kotlin
// OIDCTokenPublicKeyAdapter.kt:22
val cached = cachePort.find(key, String::class.java)
    ?: AuthException.OIDC_PUBLIC_KEY_FETCH_FROM_CACHE_FAILED.throwIt()
```

`throwIt()`의 반환 타입이 `Nothing`이므로(`ExceptionExtensions.kt:9`) 엘비스 연산자 우변에 그대로 놓을 수 있고, 이후 코드에서 값은 non-null로 좁혀진다. main 코드에 이 패턴이 19곳 있다.

`IllegalStateException`이나 `NullPointerException`으로 흘려보내지 않는다. 이유는 [error-handling.md](error-handling.md)에 정리되어 있다.

### 2.2 `!!` 사용 범위

`!!`는 전면 금지가 아니다. 현재 main 코드에 17곳이 있고, 대부분 **JPA가 생성한 식별자를 읽는 자리**다.

```kotlin
// FriendRelationMapper.kt:14
id = entity.id!!,
```

```kotlin
// NotificationRecoveryScheduler.kt:42
DeliveryCertainty.CONFIRMED -> notificationRegister.markAsSent(notification.id!!, result)
```

허용 기준은 다음과 같다.

| 상황 | 판단 |
|---|---|
| 저장 이후 읽는 JPA 식별자(`entity.id`) | 허용. 영속화 시점 이후 non-null이 보장된다 |
| 도메인 규칙상 값이 없을 수 있는 필드 | 금지. `?: throwIt()`으로 처리한다 |
| 외부 입력·응답에서 온 값 | 금지. 검증 후 사용한다 |

내부 계약 위반을 드러내야 할 때는 `requireNotNull`을 쓴다(main 11곳). 실패 사유를 남길 수 있다는 점이 `!!`와 다르다.

```kotlin
// NotificationDeliveryFacade.kt:83
val id = requireNotNull(notification.id) { "저장되지 않은 알림은 발송할 수 없습니다." }
```

---

## 3. 의존성 주입

* 생성자 주입만 사용한다. 필드 주입은 저장소에 없다.
* 주입 대상은 `private val`로 받는다.

```kotlin
// GlobalExceptionHandler.kt:26
class GlobalExceptionHandler(
    private val userErrorAlertNotifier: UserErrorAlertNotifier
)
```

### 설정값 주입

두 방식이 공존하며, **값의 개수로 구분한다.**

| 방식 | 사용 조건 | 근거 |
|---|---|---|
| `@param:Value` | 서로 관련 없는 단일 값 1~2개 | `SecurityConfig.kt:43`, `DiscordErrorAlertAdapter.kt:12` |
| `@ConfigurationProperties` | prefix로 묶이는 설정 그룹 | `ImHereJwtProperties.kt:7`(`jwt`), `OIDCProperties.kt:10`(`oidc`) |

```kotlin
// SecurityConfig.kt:43
@param:Value("\${admin.id}") private val adminId: String,
```

`@param:Value`에 `@param:` 타깃을 붙이는 이유는, Kotlin 생성자 파라미터에서 애노테이션이 프로퍼티가 아닌 파라미터에 붙도록 명시하기 위해서다.

기본값이 필요한 설정은 `${key:default}` 형태로 둔다.

```kotlin
// SecurityConfig.kt:44
@param:Value("\${management.endpoints.web.base-path:/actuator}") private val managementBasePath: String,
```

---

## 4. 트랜잭션

| 경우 | 관례 | 근거 |
|---|---|---|
| 조회 전용 서비스 | `@Transactional(readOnly = true)` (클래스 또는 메서드 수준) | `AgreementService.kt:20`, main 9곳 |
| 쓰기 유스케이스 | 메서드 수준 `@Transactional` | main 21곳 |
| 실패해도 상태 기록은 남겨야 하는 경우 | `@Transactional(propagation = Propagation.REQUIRES_NEW)` | `NotificationRegister.kt:53` |

`REQUIRES_NEW`를 쓰는 자리는 알림 발송 결과 기록이다. 바깥 트랜잭션이 롤백되어도 발송 결과는 남아야 하기 때문이다. 배경은 [../messaging.md](../messaging.md) 7장에 있다.

### 벌크 수정·삭제 쿼리

`@Modifying` 쿼리는 **호출 지점에 활성 트랜잭션이 있어야 한다.** HTTP 요청 밖(`@Scheduled`)에서는 트랜잭션이 자동으로 열리지 않아 `No active transaction for update or delete query`가 발생한다.

```kotlin
// FriendRestrictionScheduler.kt:13
@Scheduled(cron = "0 0 3 * * *")
@Transactional
fun cleanExpiredRestrictions() {
    friendRelationRepository.deleteExpired(LocalDateTime.now())
}
```

호출되는 쿼리는 `SpringDataFriendRelationRepository.kt:33`의 `@Modifying(clearAutomatically = true)` 삭제 쿼리다. `clearAutomatically = true`는 벌크 삭제 이후 영속성 컨텍스트에 남은 stale entity를 비운다.

**규칙**: `@Scheduled` 메서드가 `@Modifying` 쿼리를 호출하면 그 메서드에 `@Transactional`을 함께 붙인다.

---

## 5. 패키지와 모듈 경계

* 외부 의존성이 큰 모듈은 포트/어댑터로 분리한다(`notifications`, `auth`).
* 단순 CRUD 모듈은 계층형 MVC를 유지한다(`terms`, `agreement`).
* JPA Entity와 도메인 모델을 분리하는 모듈은 Mapper를 둔다(`NotificationMapper.kt`, `FriendRelationMapper.kt`).

모듈 경계 위반은 문서가 아니라 테스트가 강제한다. `ModularityTest.kt`와 `AuthModularityTest.kt`가 Spring Modulith 규칙으로 검증하므로, 경계를 어기면 빌드가 실패한다.

### 방향 규칙

`support` 패키지는 도메인 모듈을 참조하지 않는다. 전역 예외 처리기에서 인증·인가 응답을 분리한 이유가 이것이다.

```kotlin
// GlobalExceptionHandler.kt:125
// 인증/인가 실패(401/403)는 auth 모듈의 AuthAccessDeniedExceptionHandler가 소유한다(support→auth 순환 제거).
```

---

## 6. 테스트 작성 관례

### 네이밍

저장소에는 두 요소가 함께 쓰인다.

| 요소 | 사용량 | 역할 |
|---|---:|---|
| `@DisplayName`(한국어 서술) | 573 | 테스트 의도를 문장으로 남긴다 |
| snake_case 함수명 | 243 | 대상 메서드와 조건을 식별자로 남긴다 |
| 백틱 함수명 | 5 | 예외적으로만 사용한다 |

```kotlin
// AuthAccessDeniedExceptionHandlerTest.kt:26
@Test
@DisplayName("SecurityContext에 인증이 없으면 401과 IMHERE_INVALID_TOKEN을 반환한다")
fun handleAuthorizationDeniedException_returns_401_when_authentication_is_null() {
```

**규칙**: 새 테스트는 `@DisplayName`을 한국어로 반드시 붙이고, 함수명은 `대상_결과_조건` 형태의 snake_case로 쓴다. 백틱 이름은 새로 만들지 않는다.

### 구조

`// given` / `// when` / `// then` 주석으로 구간을 나눈다. 어느 계층에 테스트를 둘지는 [../test-strategy.md](../test-strategy.md)의 판단 순서를 따른다.

---

## 7. 코드 스멜 판단 기준

* private 메서드가 계속 늘어나면 책임 분리를 검토한다.
* 호출 순서에 의존하는 서비스는 도메인 메서드로 이동하거나 서비스를 분해한다.
* 원시 타입으로 도메인 개념을 표현하고 있으면 값 객체 도입을 검토한다.

이 항목들은 강제 규칙이 아니라 리뷰에서 질문할 신호다. 실제 리팩토링 절차는 `oop-ddd-refactor` 스킬이 다룬다.

---

## 관련 문서

* [error-handling.md](error-handling.md) — 예외를 던지고 처리하는 규칙
* [api-response-and-errors.md](api-response-and-errors.md) — 응답 형식과 에러 코드 체계
* [../test-strategy.md](../test-strategy.md) — 테스트 계층 선택 기준
* [../messaging.md](../messaging.md) — 트랜잭션 경계와 비동기 처리 규약
