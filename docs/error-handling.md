# Error Handling

## 에러 응답 구조

모든 응답이 `ApiResponse<T>`(`shared/response/ApiResponse.kt`) 하나로 통일된다.

```kotlin
data class ApiResponse<T>(
    val imhereResponseCode: String, // 성공: "SUCCESS", 실패: 도메인별 에러 코드 (예: "AUTH-100")
    val message: String,
    val data: T?
)
```

- 도메인 코드 형식: `{도메인}-{번호}` — 예) `AUTH-100`(OIDC 만료), `FRIEND-500`(이미 친구), `TOKEN-101`(유효하지 않은 토큰), `FCM-301`(등록되지 않은 토큰).
- `GlobalExceptionHandler`(`@RestControllerAdvice`, basePackages = `com.kdongsu5509`)가 모든 예외를 잡아 이 형태로 변환한다.

## 도메인 에러 코드 패턴 (`ImHereBaseErrorCode`)

도메인마다 이 인터페이스를 구현하는 enum을 하나씩 둔다(`AuthException`, `UserException`, `FriendException`, `NotificationException` 등).

```kotlin
interface ImHereBaseErrorCode {
    val httpStatus: HttpStatus
    val imhereErrorCode: String
    val errorMessage: String
}

// 실제 예 (FriendException.kt) — 상태 구간을 코드 번호 앞자리로 구분한다
enum class FriendException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    SELF_FRIENDSHIP(CommonErrorCode.INVALID_INPUT, "FRIEND-000", "자신에게는 친구 요청을 보낼 수 없습니다."),       // 0xx: 400
    FRIEND_RELATIONSHIP_OWNER_MISS_MATCH(CommonErrorCode.FORBIDDEN, "FRIEND-200", "..."),                          // 2xx: 403
    FRIEND_REQUEST_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-300", "..."),                                      // 3xx: 404
    ALREADY_FRIEND(CommonErrorCode.CONFLICT, "FRIEND-500", "이미 친구 관계입니다."),                                // 5xx: 409
    FRIEND_REQUEST_UNPROCESSABLE_BY_ME(CommonErrorCode.UNPROCESSABLE_ENTITY, "FRIEND-700", "...");                  // 7xx: 422

    override val httpStatus: HttpStatus = category.httpStatus
}
```

코드 번호 앞자리(0/2/3/5/7xx)가 HTTP 상태군과 의도적으로 대응된다(400/403/404/409/422).

## throwIt() 확장 함수 (`ExceptionExtensions.kt`)

```kotlin
fun ImHereBaseErrorCode.throwIt(
    contextData: Map<String, Any?> = emptyMap(),
    customMessage: String? = null,
    cause: Throwable? = null
): Nothing

// Bad
if (user == null) throw IllegalStateException("user not found")

// Good
val user = userRepository.findById(id) ?: UserException.USER_NOT_FOUND.throwIt()
```

`contextData`는 추가 컨텍스트를 실어 보낼 때 쓴다 — 예) FCM 전송 실패 시 `contextData["unregistered"] = true`로 표시해서 호출부가 토큰 자동 삭제 여부를 분기한다(`FCMNotificationService`).

## 운영 연동

- `GlobalExceptionHandler`는 `DiscordUserErrorNotifier`를 주입받아 일부 에러를 Discord Webhook으로도 통지한다. 자세한 내용은 [observability.md](./observability.md#alert--discord).
- Spring 표준 예외(`MethodArgumentNotValidException`, `DataIntegrityViolationException`, `HttpRequestMethodNotSupportedException` 등)도 같은 `ApiResponse` 형태로 변환된다 — 클라이언트는 도메인 예외와 프레임워크 예외를 구분할 필요가 없다.

## API 문서와의 관계

엔드포인트별로 실제 어떤 에러 코드를 반환하는지는 Spring REST Docs 기반 자동 생성 문서(`src/main/resources/static/docs/openapi3.yaml`)에 예시로 들어 있다. 이 문서는 "패턴"을, 그 OpenAPI 파일은 "엔드포인트별 실제 값"을 책임진다.
