# API Response & Error Code Convention

ImHere 서버의 **API 응답 형식과 에러 코드 체계** 규약이다.

클라이언트는 HTTP 상태 코드와 `imhereResponseCode`를 함께 사용해 결과를 판별한다. 예외를 던지고 처리하는 코드 작성 규칙은 [error-handling.md](error-handling.md)에 있다.

이 문서의 모든 형식·코드·상태는 현재 저장소 코드에서 확인한 값이다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| 응답 외형 단일화 | 성공·실패 모두 `ApiResponse<T>` | `ApiResponse.kt:3` |
| 코드 체계는 `{도메인}-{상태구간}` | 상태 구간 번호가 HTTP 상태를 결정 | `CommonErrorCode.kt:5` |
| HTTP 상태는 개별 코드가 정하지 않음 | 도메인 enum이 `CommonErrorCode`에서 위임받음 | `AuthException.kt:6` |
| 외부 API 실패는 500 고정이 아님 | 지도 API는 502/503/504를 보존 | `NaverMapProxyClient.kt:87` |
| 응답에 내부 정보를 싣지 않음 | `ApiResponse`에 `cause`·stack trace 필드가 없음 | `ApiResponse.kt:3` |
| 단, `contextData`는 응답에 실림 | 도메인 예외의 `contextData`가 실패 응답 `data`가 됨 | `GlobalExceptionHandler.kt:45` |

---

## 1. 공통 응답 형식

### 1.1 응답 구조

```kotlin
// shared/response/ApiResponse.kt:3
data class ApiResponse<T>(
    val imhereResponseCode: String,
    val message: String,
    val data: T? = null
)
```

| 필드 | 타입 | 의미 |
|---|---|---|
| `imhereResponseCode` | `String` | 성공 시 `SUCCESS`, 실패 시 공통 또는 도메인 에러 코드 |
| `message` | `String` | 클라이언트에 전달하는 안내 메시지 |
| `data` | `T?` | 성공 결과 데이터. 실패 응답에서는 예외의 `contextData` 또는 `null` |

### 1.2 성공 응답

`ApiResponse.success(data, message)`(`ApiResponse.kt:9`)가 성공 응답을 만들며 기본 메시지는 `OK`다. controller가 `ApiResponse`가 아닌 값을 반환해도 `GlobalResponseHandler`(`GlobalResponseHandler.kt:35`)가 감싼다.

```json
{
  "imhereResponseCode": "SUCCESS",
  "message": "OK",
  "data": { "id": 42 }
}
```

| 확장 함수 | 동작 | 근거 |
|---|---|---|
| `toOkResponse()` | 200 + `message: "OK"` | `APIResponseExtensions.kt:6` |
| `toSuccessResponse(status)` | 지정 상태 + `message: status.reasonPhrase` | `APIResponseExtensions.kt:9` |
| `toFailResponse(status, code, message?)` | 지정 상태 + 실패 코드, 수신 객체가 `data`가 됨 | `APIResponseExtensions.kt:15` |

### 1.3 실패 응답

```json
{
  "imhereResponseCode": "TOKEN-101",
  "message": "유효하지 않은 토큰입니다.",
  "data": null
}
```

서버는 `cause`, stack trace, 토큰, 외부 서비스 자격 증명과 원문 응답을 API에 노출하지 않는다. `ApiResponse`에 해당 필드가 없기 때문에 구조적으로 실리지 않는다.

**단, `contextData`는 예외다.** `toFailResponse`의 수신 객체가 `data`가 되므로, `GlobalExceptionHandler.kt:45`가 도메인 예외의 `contextData`를 그대로 응답에 담는다. `contextData`에 무엇을 넣을지는 [error-handling.md](error-handling.md#13-contextdata-사용-규칙)의 규칙을 따른다.

---

## 2. 응답을 만드는 지점

| 지점 | 담당 범위 | 근거 |
|---|---|---|
| `GlobalResponseHandler` | 성공 응답 래핑 | `GlobalResponseHandler.kt:13` |
| `GlobalExceptionHandler` | 도메인 예외 + MVC/validation 예외 | `GlobalExceptionHandler.kt:25` |
| `AuthAccessDeniedExceptionHandler` | controller 진입 이후 인가 거부 | `AuthAccessDeniedExceptionHandler.kt:16` |
| `SecurityConfig` handler 람다 | filter 단계 인증·인가 거부 | `SecurityConfig.kt:127` |
| controller local `@ExceptionHandler` | 지도 프록시 예외 | `NaverMapProxyController.kt` |

각 지점의 상세 동작과 알려진 예외는 [error-handling.md](error-handling.md#3-예외를-응답으로-바꾸는-지점)에 정리되어 있다. 여기서 알아야 할 것은 **경로에 따라 응답을 만드는 주체가 다르다**는 점이다.

---

## 3. 에러 코드 체계

에러 코드는 `{도메인}-{상태 구간}` 형식이다. 기준 정의는 `CommonErrorCode`(`CommonErrorCode.kt:5`)와 각 도메인 enum이다.

### 3.1 상태 구간

| 상태 구간 | HTTP 상태 | 의미 | 공통 코드 |
|---:|---:|---|---|
| `0xx` | 400 | 잘못된 요청·입력 | `GLOBAL-000`, `GLOBAL-001` |
| `1xx` | 401 | 인증 실패 | `GLOBAL-100` |
| `2xx` | 403 | 인가 실패 | `GLOBAL-200` |
| `3xx` | 404 | 리소스 없음 | `GLOBAL-300` |
| `4xx` | 405 | 허용되지 않은 HTTP 메서드 | `GLOBAL-400` |
| `5xx` | 409 | 상태 충돌·중복 요청 | `GLOBAL-500` |
| `6xx` | 415 | 지원하지 않는 media type | `GLOBAL-600` |
| `7xx` | 422 | 처리할 수 없는 요청 | `GLOBAL-700` |
| `9xx` | 500 | 서버·인프라·외부 연동 실패 | `GLOBAL-900`, `GLOBAL-901` |

`GLOBAL-900`(`INFRA_FAILURE`)과 `GLOBAL-901`(`INTERNAL_SERVER_ERROR`)은 둘 다 500이지만 의미가 다르다. 외부 통신 실패는 `900`, 서버 내부 오류는 `901`이다.

### 3.2 도메인 prefix

| 도메인 | Prefix | 정의 위치 |
|---|---|---|
| 공통 | `GLOBAL` | `support/exception/CommonErrorCode.kt` |
| 인증·토큰 | `AUTH`, `TOKEN` | `auth/AuthException.kt` |
| 사용자 | `USER` | `user/exception/UserException.kt` |
| 친구 관계 | `FRIEND` | `friends/FriendException.kt` |
| 약관·동의 | `AGREEMENT` | `agreement/AgreementException.kt` |
| 알림·FCM·SMS | `NOTI`, `FCM`, `SMS` | `notifications/exception/NotificationException.kt` |
| 지도 외부 API | `MAP` | `maps/NaverMapProxyClient.kt` (enum 아님) |

`MAP` 코드만 enum이 아니라 호출 지점의 문자열 리터럴이다. `NaverMapProxyException`(`NaverMapProxyException.kt:5`)은 `ImHereBaseErrorCode`를 구현하지 않고 `status`, `code`, `message`를 직접 들고 다니는 `RuntimeException`이기 때문이다. 따라서 이 예외는 `GlobalExceptionHandler`의 `ImHereBaseException` 경로를 타지 않는다.

### 3.3 새 코드를 추가하는 규칙

1. 공통 오류면 `CommonErrorCode`, 도메인 규칙 위반이면 해당 도메인 enum에 추가한다.
2. 기존 prefix와 상태 구간 번호 규칙을 유지한다.
3. enum 첫 인자에 해당 상태의 `CommonErrorCode`를 지정한다. HTTP 상태를 직접 쓰지 않는다.
4. 메시지는 클라이언트에게 그대로 노출되므로 내부 구현 용어를 넣지 않는다.
5. 새 코드가 클라이언트 분기에 영향을 주면 이 문서의 4장에 대응 방법을 함께 기록한다.

---

## 4. 오류 유형별 처리

### 4.1 인증 실패 — 401

인증 정보가 없거나 유효하지 않은 경우다. OIDC 토큰 만료·형식·서명 오류, 유효하지 않은 ImHere 토큰, 비활성·잠금·탈퇴 계정 등이 해당한다.

대표 코드: `AUTH-100`~`AUTH-109`, `TOKEN-100`~`TOKEN-103`, `GLOBAL-100`

```json
{
  "imhereResponseCode": "TOKEN-101",
  "message": "유효하지 않은 토큰입니다.",
  "data": null
}
```

클라이언트는 토큰 갱신 또는 재로그인을 수행한다. 서버는 구체적인 검증 단계나 내부 키 정보를 공개하지 않는다.

> `/api/admin/**`에 토큰 없이 접근하면 현재 `sendError(401, "Unauthorized")`가 응답되어 공통 JSON 형식이 아니다(`SecurityConfig.kt:104`).

### 4.2 인가 실패 — 403

인증은 되었지만 권한이 없는 경우다.

대표 코드: `AUTH-200`, `FRIEND-200`, `NOTI-200`, `GLOBAL-200`

```json
{
  "imhereResponseCode": "AUTH-200",
  "message": "해당 기능에 대한 권한이 없습니다.",
  "data": null
}
```

filter 단계 거부는 `AUTH-200`(`SecurityConfig.kt:128`), controller 진입 이후 거부는 처리기에 따라 `AUTH-200` 또는 `GLOBAL-200`이 될 수 있다. 처리기 우선순위 이슈는 [error-handling.md](error-handling.md#34-controller-진입-이후의-인가-거부)에 기록되어 있다.

### 4.3 리소스 없음 — 404

조회 대상이 없을 때 도메인별 코드를 사용한다.

대표 코드: `AUTH-300`, `USER-300`, `FRIEND-300`, `AGREEMENT-300`, `NOTI-300`, `FCM-300`, `GLOBAL-300`

존재하지 않는 리소스를 임의로 생성하거나 성공으로 처리하지 않는다. 라우팅 대상 자체가 없으면 `GLOBAL-300`(`GlobalExceptionHandler.kt:105`), 도메인 조회 실패는 해당 도메인 코드를 사용한다.

### 4.4 중복 요청·상태 충돌 — 409

대표 코드: `USER-500`, `FRIEND-500`, `FRIEND-501`, `GLOBAL-500`

```json
{
  "imhereResponseCode": "FRIEND-501",
  "message": "이미 친구 요청을 보낸 상태입니다.",
  "data": null
}
```

도메인 예외는 구체적인 코드를 사용한다. DB 제약에서 올라온 `DataIntegrityViolationException`은 전역 처리기가 `GLOBAL-500`으로 변환한다(`GlobalExceptionHandler.kt:128`).

### 4.5 처리 불가 — 422

요청 형식은 맞지만 현재 상태에서 처리할 수 없는 경우다.

대표 코드: `GLOBAL-700`, `FRIEND-700`, `AGREEMENT-700`, `AGREEMENT-701`

`throwIt()`은 이 상태에 대해 전용 타입 `UnprocessableEntityException`을 생성한다(`ExceptionExtensions.kt`).

### 4.6 외부 API 실패

외부 API 실패는 항상 500으로 고정되지 않는다. 외부 시스템의 특성에 따라 상태와 코드를 보존한다.

| 연동 | 상태 | 코드 | 의미 | 근거 |
|---|---:|---|---|---|
| Naver 지도 | 502 | `MAP-502` | 외부 서비스 연결·호출 실패 | `NaverMapProxyClient.kt:94` |
| Naver 지도 | 503 | `MAP-503` | 외부 API 자격 설정 누락 | `NaverMapProxyClient.kt:105` |
| Naver 지도 | 504 | `MAP-504` | 외부 API 응답 시간 초과 | `NaverMapProxyClient.kt:87` |
| FCM | 500 | `FCM-900`~`FCM-902` | 메시지 구성·인증·알 수 없는 오류 | `NotificationException.kt:29` |
| FCM | 500 | `FCM-903` | 재시도 대상 일시 오류 | `NotificationException.kt:32` |
| SMS | 500 | `SMS-900` | 외부 SMS 전송 실패 | `NotificationException.kt:33` |

지도 프록시는 `NaverMapProxyClient`가 `ResourceAccessException`과 `RestClientException`을 `NaverMapProxyException`으로 변환하고, controller의 local `@ExceptionHandler`가 상태·코드·메시지를 `ApiResponse.fail(...)`로 반환한다.

FCM과 SMS 실패는 HTTP 응답이 아니라 `Notification` 상태로 기록된다. 외부 서비스의 원문 응답과 자격 증명은 클라이언트에 전달하지 않는다.

### 4.7 비동기 알림 실패

친구 요청 등 도메인 이벤트를 `@ApplicationModuleListener`가 수신한 뒤 알림을 접수·발송한다. 이 작업은 HTTP 응답 이후에 진행되므로 **발송 실패는 기존 HTTP 응답으로 전달되지 않는다.**

클라이언트가 알아야 할 것은 다음 두 가지다.

* 알림 관련 API가 200을 반환해도 그것은 **접수 성공**이지 발송 성공이 아니다.
* 발송 결과는 알림 조회 API의 상태 값으로 확인한다.

| 상태 | 의미 | 근거 |
|---|---|---|
| `PENDING` | 발송 가능 상태이며 아직 완료 전 | `NotificationStatus.kt:11` |
| `PROCESSING` | 한 실행 흐름이 발송 소유권을 잡은 상태 | `SpringDataNotificationRepository.kt:23` |
| `SENT` | 외부 채널 발송 성공 | — |
| `FAILED` | 실패했지만 재시도 가능(`isSendable()`) | `NotificationStatus.kt:11` |
| `UNKNOWN` | 발송 여부를 확신할 수 없음 | [../messaging.md](../messaging.md) 7장 |
| `DEAD` | `MAX_ATTEMPTS = 3` 도달, 자동 재시도 중단 | `Notification.kt:29`, `Notification.kt:133` |

재시도 정책, 중복 방지, 복구 스케줄러, 보장 범위와 한계는 [../messaging.md](../messaging.md)가 단일 출처다. 이 문서에서는 중복 기술하지 않는다.

---

## 5. 클라이언트·서버 운영 규칙

* HTTP 상태 코드만으로 분기하지 말고 `imhereResponseCode`를 함께 확인한다.
* 인증 실패는 토큰 갱신·재로그인, 인가 실패는 권한 확인, 404는 대상 존재 여부 확인, 409는 중복·현재 상태 확인으로 대응한다.
* 서비스 계층에서는 일반 `IllegalStateException` 대신 도메인 코드와 `throwIt()`을 사용한다.
* `contextData`에는 클라이언트에 노출되어도 되는 최소 식별 정보만 넣는다.
* 외부 오류는 재시도 가능 여부를 구분한다. 지도 API는 호출 경계에서 502/503/504를 반환하고, FCM 재시도는 `RetryableFcmException`에 한정된다.
* 새 에러 코드는 관련 enum에 추가하고, HTTP 상태·메시지·클라이언트 대응·재시도 여부를 이 문서에 함께 기록한다.

---

## 6. 구현 근거

| 영역 | 구현 위치 |
|---|---|
| 응답 모델·성공/실패 생성 | `shared/response/ApiResponse.kt`, `APIResponseExtensions.kt` |
| 필터·security 응답 직렬화 | `shared/response/APIResponseSerializers.kt`, `auth/security/config/SecurityConfig.kt`, `auth/security/filter/JwtAuthenticationFilter.kt` |
| 전역 예외·성공 응답 래핑 | `support/handler/GlobalExceptionHandler.kt`, `GlobalResponseHandler.kt`, `SecurityExceptionHandler.kt` |
| 인증·인가 controller 예외 | `auth/adapter/in/web/AuthAccessDeniedExceptionHandler.kt` |
| 공통 코드·예외 생성 | `support/exception/CommonErrorCode.kt`, `ImHereBaseErrorCode.kt`, `ImHereBaseException.kt`, `ExceptionExtensions.kt`, `type/` |
| 도메인 코드 | `auth/AuthException.kt`, `user/exception/UserException.kt`, `friends/FriendException.kt`, `agreement/AgreementException.kt`, `notifications/exception/NotificationException.kt` |
| 지도 외부 API 오류 | `maps/NaverMapProxyClient.kt`, `NaverMapProxyController.kt`, `NaverMapProxyException.kt` |
| 알림 상태·재시도·복구 | `notifications/domain/Notification.kt`, `NotificationStatus.kt`, `NotificationDeliveryFacade.kt`, `NotificationRegister.kt`, `NotificationRecoveryScheduler.kt` |

---

## 관련 문서

* [error-handling.md](error-handling.md) — 예외를 던지고 처리하는 코드 규칙
* [kotlin-conventions.md](kotlin-conventions.md) — null 처리, 트랜잭션, 주입 규칙
* [../messaging.md](../messaging.md) — 비동기 알림 보장 범위와 한계
* [../test-strategy.md](../test-strategy.md) — 응답·예외 경로의 테스트 계층
