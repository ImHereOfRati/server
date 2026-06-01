---
name: error-handling
description: ImHere Server 프로젝트의 전역 예외 처리 규칙. ErrorCode Enum 정의, .throwIt() 확장 함수 사용법 등을 규정한다.
---

# Error Handling Rules

## Pre-task Checklist

- [ ] 도메인 특화 예외는 개별 Bounded Context에 `Enum` 클래스로 정의했는가?
- [ ] 예외를 던질 때 `throw RuntimeException` 대신 도메인 에러 코드의 `.throwIt()` 확장 함수를 사용했는가?

## Rule 1 — Define Domain Error Codes

비즈니스 예외 코드는 개별 도메인(Bounded Context) 내의 `exception` 패키지에 `Enum` 클래스로 정의하고, `ImHereBaseErrorCode`를 구현해야 합니다. HTTP 상태 코드는 `CommonErrorCode`를 활용하여 매핑합니다.

```kotlin
// 예시: src/main/kotlin/com/kdongsu5509/user/exception/UserException.kt
enum class UserException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    
    USER_NOT_FOUND(CommonErrorCode.NOT_FOUND, "USER-300", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(CommonErrorCode.CONFLICT, "USER-500", "이미 사용 중인 이메일입니다.");

    override val httpStatus: HttpStatus = category.httpStatus
}
```

## Rule 2 — Throwing Exceptions

예외를 발생시킬 때는 `throw NotFoundException()`과 같이 직접 예외 객체를 생성하지 않고, **Enum 항목에 정의된 `.throwIt()` 확장 함수를 호출**해야 합니다. 
내부적으로 상태 코드에 맞는 구체적인 예외 클래스로 자동 변환되어 던져집니다. (엘비스 연산자 `?:` 와 함께 사용하면 매우 유용합니다.)

```kotlin
// ✅ 권장 (확장 함수 사용)
fun findUser(id: Long): User {
    return userRepository.findByIdOrNull(id) 
        ?: UserException.USER_NOT_FOUND.throwIt()
}

// ❌ 금지 (직접 예외 클래스 생성)
fun findUser(id: Long): User {
    return userRepository.findByIdOrNull(id) 
        ?: throw RuntimeException("사용자를 찾을 수 없습니다.")
}
```

## Rule 3 — Global Exception Handler

`throwIt()`으로 던져진 예외는 `GlobalExceptionHandler`에 의해 자동으로 캐치되며, 클라이언트에게 일관된 포맷(에러 코드, 메시지 등)의 `ApiResponse`로 반환됩니다. 따라서 Controller나 Service 레벨에서 HTTP 응답용으로 개별적인 `try-catch` 블록을 작성하지 않습니다.
