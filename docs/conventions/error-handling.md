# Error Handling Convention

ImHere 서버에서 **예외를 어떻게 던지고, 어디에서 응답으로 바꾸는지**에 대한 규칙이다.

응답 JSON 형식과 에러 코드 목록 자체는 [api-response-and-errors.md](api-response-and-errors.md)에 있다. 이 문서는 코드를 작성하는 쪽의 규칙만 다룬다.

이 문서의 모든 규칙은 현재 저장소 코드에서 확인한 사실에만 근거한다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 예외는 도메인 에러 코드에서 생성 | `ImHereBaseErrorCode.throwIt()`이 HTTP 상태에 맞는 예외 타입을 고름 | `ExceptionExtensions.kt:5` |
| 서비스는 HTTP를 모름 | `HttpStatus`와 직렬화 규칙은 에러 코드 enum과 전역 처리기에만 존재 | `AuthException.kt:6`, `GlobalExceptionHandler.kt:31` |
| 도메인 코드는 `CommonErrorCode`에서 상태를 위임받음 | enum 첫 인자가 `CommonErrorCode`이고 `httpStatus`를 그대로 가져감 | `AuthException.kt:6`, 마지막 줄 |
| 프레임워크 예외도 같은 외형으로 변환 | validation, MVC, 무결성 위반 예외를 `ApiResponse`로 통일 | `GlobalExceptionHandler.kt:54` |
| 인증·인가 응답은 `auth` 모듈이 소유 | `support`가 도메인 모듈을 참조하지 않도록 분리 | `GlobalExceptionHandler.kt:125` |
| `contextData`는 응답에 실린다 | `ImHereBaseException`의 `contextData`가 실패 응답 `data`가 됨 | `GlobalExceptionHandler.kt:45` |

---

## 1. 예외를 던지는 규칙

### 1.1 도메인 코드로 던진다

서비스와 도메인 코드는 `throwIt()`으로 예외를 만든다. `IllegalStateException`, `IllegalArgumentException`, `NullPointerException`을 그대로 전파하지 않는다.

```kotlin
// OIDCTokenPublicKeyAdapter.kt:22
val cached = cachePort.find(key, String::class.java)
    ?: AuthException.OIDC_PUBLIC_KEY_FETCH_FROM_CACHE_FAILED.throwIt()
```

이 패턴을 쓰는 이유는 **서비스 코드가 HTTP 상태와 응답 직렬화 규칙을 몰라도 되게 만들기 위해서**다. 상태 결정은 에러 코드 enum이, 직렬화는 전역 처리기가 담당한다.

### 1.2 `throwIt()`이 고르는 예외 타입

`ExceptionExtensions.kt:5`의 `throwIt()`은 에러 코드의 `httpStatus.value()`를 보고 예외 타입을 고른다.

| HTTP 상태 | 생성되는 예외 |
|---:|---|
| 400 | `InvalidInputException` |
| 401 | `UnauthorizedException` |
| 403 | `ForbiddenException` |
| 404 | `NotFoundException` |
| 409 | `ConflictException` |
| 422 | `UnprocessableEntityException` |
| 500 | `InternalServerException` |
| 그 외(405, 415 등) | `ImHereBaseException` |

전용 타입이 없는 상태도 에러 코드의 HTTP 상태와 메시지는 그대로 유지되므로 응답 결과는 달라지지 않는다. 전용 타입은 `support/exception/type/`에 7개가 있다.

반환 타입이 `Nothing`(`ExceptionExtensions.kt:9`)이라 엘비스 연산자 우변에 그대로 놓을 수 있다.

### 1.3 `contextData` 사용 규칙

`throwIt(contextData = ...)`에 넘긴 값은 **로그 전용이 아니다.** 전역 처리기가 이 값을 그대로 실패 응답의 `data`로 내보낸다.

```kotlin
// GlobalExceptionHandler.kt:45
return e.contextData.toFailResponse(
    status = errorCode.httpStatus,
    imhereErrorCode = errorCode.imhereErrorCode,
    errorMessage = e.message
)
```

따라서 규칙은 다음과 같다.

* 토큰, 비밀번호, 외부 서비스 자격 증명, 외부 응답 원문을 넣지 않는다.
* 클라이언트가 후속 분기에 쓸 수 있는 최소 식별 정보만 넣는다(예: FCM 토큰 만료 여부).
* 넣기 전에 **"이 값이 클라이언트에게 그대로 보여도 되는가"** 를 판단한다.

`cause`와 stack trace는 응답에 포함되지 않는다. `ApiResponse`에 해당 필드가 없기 때문이다(`ApiResponse.kt:3`).

---

## 2. 새 에러 코드를 추가하는 규칙

도메인 enum은 `ImHereBaseErrorCode`를 구현하고, 첫 인자로 `CommonErrorCode`를 받아 HTTP 상태를 위임한다.

```kotlin
// AuthException.kt:6
enum class AuthException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    OIDC_EXPIRED(CommonErrorCode.UNAUTHORIZED, "AUTH-100", "OIDC ID 토큰이 만료되었습니다."),
    // ...
    override val httpStatus = category.httpStatus
}
```

이 구조 덕분에 **HTTP 상태를 개별 코드가 직접 정하지 않는다.** 상태 구간과 코드 번호가 어긋나는 실수를 막기 위한 설계다.

추가 절차:

1. 공통 오류면 `CommonErrorCode`, 도메인 규칙 위반이면 해당 도메인 enum에 추가한다.
2. 기존 prefix와 상태 구간 규칙을 유지한다(→ [api-response-and-errors.md](api-response-and-errors.md) 3장).
3. `category`에 해당 상태의 `CommonErrorCode`를 지정한다.
4. 메시지는 클라이언트에게 그대로 보이는 문장으로 쓴다. 내부 구현 용어를 넣지 않는다.

---

## 3. 예외를 응답으로 바꾸는 지점

응답 변환은 네 곳에 나뉘어 있다. **어느 지점이 처리하는지에 따라 응답 외형이 달라질 수 있으므로**, 새 예외를 추가할 때 어느 경로를 타는지 확인한다.

| 처리기 | 위치 | 담당 |
|---|---|---|
| `GlobalExceptionHandler` | `support/handler/GlobalExceptionHandler.kt:25` | `ImHereBaseException` + MVC/validation/무결성 예외 |
| `GlobalResponseHandler` | `support/handler/GlobalResponseHandler.kt:13` | 성공 응답 래핑 |
| `AuthAccessDeniedExceptionHandler` | `auth/adapter/in/web/AuthAccessDeniedExceptionHandler.kt:16` | controller 진입 이후의 인가 거부 |
| `SecurityConfig`의 handler 람다 | `auth/security/config/SecurityConfig.kt:104` 외 | filter 단계의 인증·인가 거부 |

### 3.1 `GlobalExceptionHandler`

`@RestControllerAdvice(basePackages = ["com.kdongsu5509"])`로 동작한다(`:25`).

| 대상 | 응답 코드·상태 | 근거 |
|---|---|---|
| `ImHereBaseException` | 예외가 가진 도메인 코드·상태 | `:31` |
| `MethodArgumentNotValidException` | `GLOBAL-000`, 400 (필드별 메시지 결합) | `:54` |
| 타입 불일치·파라미터 누락·제약 위반 | `GLOBAL-000`, 400 | `:68` |
| `HttpMessageNotReadableException` | `GLOBAL-001`, 400 | `:86` |
| `NoResourceFoundException` | `GLOBAL-300`, 404 | `:105` |
| `HttpRequestMethodNotSupportedException` | `GLOBAL-400`, 405 | `:116` |
| `DataIntegrityViolationException` | `GLOBAL-500`, 409 | `:128` |
| `HttpMediaTypeNotSupportedException` | `GLOBAL-600`, 415 | `:139` |
| 그 밖의 `Exception` | `GLOBAL-901`, 500 | `:149` |

두 가지 동작을 함께 한다.

* `ImHereBaseException` 처리 시 `userErrorAlertNotifier.notifyUserError(...)`로 운영 알림을 보낸다(`:39`).
* 요청 body 파싱 중 발생한 예외라도 root cause가 `ImHereBaseException`이면 도메인 응답으로 위임한다(`:92`). 역직렬화 안에서 도메인 검증이 실패해도 `GLOBAL-001`로 뭉개지지 않게 하려는 처리다.

### 3.2 `GlobalResponseHandler`

controller 반환값을 `ApiResponse`로 감싼다(`:35`). 이미 `ApiResponse`이거나 `ResponseEntity<ApiResponse<*>>`이면 다시 감싸지 않는다(`:22`, `:26`). springdoc/swagger controller는 제외한다(`:30`).

따라서 controller는 `ApiResponse`를 직접 만들어도 되고 도메인 DTO를 그대로 반환해도 되며, 최종 성공 응답 외형은 같다.

### 3.3 인증·인가 경계

인증 필터와 Spring Security의 exception handling은 MVC 전역 예외 처리보다 **먼저** 동작한다. 그래서 이 경로의 응답은 `GlobalExceptionHandler`가 아니라 SecurityConfig의 handler가 만든다.

| 보안 체인 | 인증 실패(entry point) | 인가 실패(access denied) |
|---|---|---|
| `/api/**` (`SecurityConfig.kt:113`) | `ApiResponse` JSON, `TOKEN-101`, 401 (`:127`) | `ApiResponse` JSON, `AUTH-200`, 403 (`:128`) |
| `/api/admin/**` (`:91`) | **`sendError(401, "Unauthorized")`** (`:104`) | `ApiResponse` JSON, `AUTH-200`, 403 (`:105`) |
| `/admin`, `/admin/**` (`:68`) | `:81` (웹 로그인 화면 경로) | `sendError(403)` (`:84`) |

JSON 직렬화는 `APIResponseSerializers.writeErrorResponse(...)`(`APIResponseSerializers.kt:13`)가 담당한다.

> **알려진 구현 예외**
> `/api/admin/**`의 인증 정보 누락 응답은 `sendError`를 사용하므로 공통 `ApiResponse` JSON 형식이 아니다(`SecurityConfig.kt:104`). 관리자 API까지 공통 형식이 필요하면 이 지점을 `APIResponseSerializers.writeErrorResponse(...)`로 바꿔야 한다.

### 3.4 controller 진입 이후의 인가 거부

`@PreAuthorize` 등으로 controller 진입 이후 거부되면 `AuthorizationDeniedException`이 발생한다. `AuthAccessDeniedExceptionHandler`(`:16`)는 SecurityContext가 익명인지 보고 401 또는 403을 고른다(`:25`).

```kotlin
// AuthAccessDeniedExceptionHandler.kt:25
if (isAnonymousAuthentication(authentication))
    return null.toFailResponse(
        status = HttpStatus.UNAUTHORIZED,
        imhereErrorCode = AuthException.IMHERE_INVALID_TOKEN.imhereErrorCode,
        errorMessage = "인증이 필요합니다."
    )
```

> **주의: 처리기 두 개가 같은 예외를 선언하고 있다**
> `SecurityExceptionHandler`(`support/handler/SecurityExceptionHandler.kt:15`, `@Order(3)`)도 `AuthorizationDeniedException`을 처리하며 항상 `GLOBAL-200`·403을 반환한다. `AuthAccessDeniedExceptionHandler`에는 `@Order`가 없어 기본 우선순위가 가장 낮다.
> 두 advice 중 어느 쪽이 선택되는지는 우선순위에 달려 있고, 현재 이 조합을 검증하는 통합 테스트는 없다. `AuthAccessDeniedExceptionHandlerTest.kt:19`는 핸들러를 직접 호출하는 단위 테스트라 advice 선택 순서를 확인하지 않는다.
> **인가 거부 응답 형식을 바꾸는 작업을 할 때는 두 처리기를 함께 확인한다.**

---

## 4. 스케줄러·비동기 경계의 예외

* `@Scheduled` 메서드 안에서 발생한 예외는 HTTP 응답으로 나가지 않는다. 상태 기록과 로그로만 남는다.
* 알림 발송 실패는 예외 전파가 아니라 `Notification` 상태(`FAILED` / `UNKNOWN` / `DEAD`)로 표현한다. 규약은 [../messaging.md](../messaging.md) 3장에 있다.
* `@Scheduled` + `@Modifying` 조합의 트랜잭션 요구사항은 [kotlin-conventions.md](kotlin-conventions.md#4-트랜잭션)에 있다.

---

## 5. 규칙 요약

| 상황 | 규칙 |
|---|---|
| 도메인 규칙 위반 | 도메인 enum + `throwIt()` |
| null 확인 실패 | `?: <도메인코드>.throwIt()` |
| 외부 연동 실패 | 재시도 가능 여부를 구분해 예외 타입을 나눈다 (`RetryableFcmException`) |
| 프레임워크 예외 | 직접 잡지 않는다. 전역 처리기에 맡긴다 |
| 새 에러 코드 | 도메인 enum에 추가하고 `CommonErrorCode`로 상태를 위임한다 |
| `contextData` | 클라이언트에 노출되어도 되는 값만 넣는다 |
| 인가 응답 변경 | `SecurityConfig`, `AuthAccessDeniedExceptionHandler`, `SecurityExceptionHandler` 세 곳을 함께 확인한다 |

---

## 관련 문서

* [api-response-and-errors.md](api-response-and-errors.md) — 응답 형식과 에러 코드 전체 목록
* [kotlin-conventions.md](kotlin-conventions.md) — null 처리, 트랜잭션, 주입 규칙
* [../test-strategy.md](../test-strategy.md) — 예외 경로를 어느 계층에서 검증할지
