# Error Handling

ImHere 서버는 성공과 실패를 같은 응답 외형으로 감싸고, 예외를 도메인 코드 중심으로 분류합니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 응답 포맷 단일화 | 모든 API 응답을 `ApiResponse<T>`로 감쌈 | `ApiResponse.kt` |
| 예외는 도메인 코드 중심 | 도메인별 enum + `throwIt()` 사용 | `ExceptionExtensions.kt:8` |
| 프레임워크 예외도 같은 외형 | validation, MVC, security 예외도 통일 응답 | `GlobalExceptionHandler.kt:60` |

---

## 응답 구조

```kotlin
data class ApiResponse<T>(
    val imhereResponseCode: String,
    val message: String,
    val data: T?
)
```

`imhereResponseCode`는 성공이면 `SUCCESS`, 실패면 도메인별 에러 코드입니다.

---

## 에러 코드 구조

| 항목 | 예시 |
|---|---|
| 인증 | `AUTH-100` |
| 친구 | `FRIEND-500` |
| FCM | `FCM-301` |

도메인별 enum:

* `AuthException`
* `FriendException`
* `NotificationException`
* `UserException`

---

## 예외 생성 방식

서비스/도메인 코드는 `throwIt()`로 HTTP 상태별 예외 타입을 감쌉니다.

```kotlin
val user = userRepository.findByEmail(email)
    ?: AuthException.USER_NOT_REGISTER.throwIt()
```

이 패턴의 목적은 서비스 코드가 `HttpStatus`와 응답 직렬화 규칙을 몰라도 되게 만드는 것입니다.

---

## 전역 처리기

### `GlobalExceptionHandler`

처리 대상:

* `ImHereBaseException`
* validation 예외
* Spring MVC 예외
* 일반 `Exception`

역할:

* `ApiResponse`로 직렬화
* 일부 사용자 에러를 Discord로 통지

### `SecurityExceptionHandler`

* 인증/인가 실패 응답 보조 처리

### `GlobalResponseHandler`

* 성공 응답 래핑

---

## 실제 실패 응답 예시

```json
{
  "imhereResponseCode": "AUTH-300",
  "message": "가입되지 않은 사용자입니다.",
  "data": null
}
```

---

## 운영 규칙

* 프레임워크 예외도 동일 응답 포맷으로 변환합니다.
* 인증 실패와 권한 실패도 도메인 코드로 통일합니다.
* context 데이터는 FCM 토큰 만료 같은 후속 분기 판단에 사용합니다.

---

## 코드 근거

* `throwIt()` 확장: `src/main/kotlin/com/kdongsu5509/support/exception/ExceptionExtensions.kt:8`
* 비즈니스 예외 래핑: `src/main/kotlin/com/kdongsu5509/support/handler/GlobalExceptionHandler.kt:34`
* validation 예외 통합: `src/main/kotlin/com/kdongsu5509/support/handler/GlobalExceptionHandler.kt:60`

---

## 관련 문서

* 구현 관례: [kotlin-conventions.md](kotlin-conventions.md)
* 테스트 구조: [testing.md](testing.md)
