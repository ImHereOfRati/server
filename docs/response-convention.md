# 📝 응답 및 에러 처리 표준 컨벤션 (Response & Error Convention)

본 문서는 프로젝트의 일관된 API 응답 구조와 효율적인 에러 진단을 위한 표준 규격을 정의합니다. 모든 컨트롤러와 예외 처리는 본 컨벤션을 준수해야 합니다.

---

## 1. 응답 구조 (APIResponseBody)

모든 API 응답은 `APIResponseBody` 클래스로 감싸서 반환하며, HTTP 상태 코드는 Header에, 비즈니스 식별 코드는 Body에 포함합니다.

### **데이터 구조**
```json
{
  "code": "SUCCESS",            // 비즈니스 식별 코드 (문자열)
  "message": "OK",              // 사람이 읽을 수 있는 메시지
  "data": { ... }               // 실제 응답 데이터 (없을 경우 null)
}
```

### **필드 상세**
*   **`code`**: 클라이언트가 에러 종류를 식별하기 위한 고유 키. 성공 시 `"SUCCESS"`, 실패 시 `GLOBAL-300` 등 `ErrorReason`에 정의된 코드를 사용합니다.
*   **`message`**: 응답에 대한 요약 설명. 에러 발생 시 개발자가 명시한 `detail` 메시지가 우선적으로 담깁니다.
*   **`data`**: 실제 전송 데이터. 에러 발생 시에는 에러가 발생한 컨텍스트 데이터(Map 형태)가 담길 수 있습니다.

---

## 2. 성공 응답 작성법 (Success Response)

Kotlin의 **확장 함수**를 사용하여 간결하게 `ResponseEntity`를 생성합니다.

### **기본 사용법**
```kotlin
// 200 OK 응답 (가장 많이 사용)
return userService.getUser(id).toOkResponse()

// 201 Created 등 특정 상태 코드 응답
return newUser.toSuccessResponse(HttpStatus.CREATED)

// 데이터가 없는 경우 (204 No Content)
return null.toSuccessResponse(HttpStatus.NO_CONTENT)
```

### **확장 함수 사용 이유 (RATIONALE)**
단순 생성자가 아닌 확장 함수를 사용하는 이유는 다음과 같습니다.

1.  **개발자 경험(DX) 향상**: 데이터 객체에서 즉시 `toOkResponse()`를 호출하여 응답으로 변환할 수 있어 흐름이 매우 간결합니다.
2.  **직관적인 명칭**: `toOkResponse`, `toSuccessResponse`, `toFailResponse` 등 함수의 이름을 통해 응답의 성격을 즉시 파악할 수 있습니다.
3.  **Null 안정성**: `T?` 타입에 정의되어 있어, 데이터가 `null`인 경우에도 일관된 문법을 유지합니다.
4.  **관심사 분리**: 도메인 객체가 웹 응답 규격(`APIResponseBody`)이나 HTTP 상태 코드에 대해 직접 알 필요 없이, 공통 모듈을 통해 응답 규격을 강제합니다.

---

## 3. 에러 처리 패턴 (Error Handling)

Loki를 통한 로그 중심 진단을 위해 **시맨틱 예외(Semantic Exception)**와 **비즈니스 에러 Enum**을 조합하여 사용합니다.

### **비즈니스 에러 Enum (`BusinessCode`)**
각 도메인별로 세부 에러 코드를 Enum으로 관리하며, `throwIt()` 확장 함수를 통해 적절한 시맨틱 예외를 발생시킵니다.

```kotlin
interface BusinessCode {
    val errorCategory: ErrorReason
    val businessCode: String
    val message: String? // 선택 사항: null인 경우 errorCategory의 기본 메시지 사용
}
```

#### **메시지 결정 우선순위**
`throwIt()` 호출 시 최종 응답 메시지는 다음 순서에 따라 결정됩니다:
1.  **`customMessage`**: 호출 시 직접 전달한 메시지 (최우선)
2.  **`Enum.message`**: Enum 상수에 정의된 메시지
3.  **`ErrorReason.defaultMessage`**: 해당 카테고리의 기본 공통 메시지 (최하위)

---

### **예외 클래스 종류 (시맨틱 카테고리)**
모든 `ErrorReason`은 고유의 **기본 메시지(`defaultMessage`)**를 가지고 있어, 별도의 메시지 지정 없이도 친절한 응답이 가능합니다.

| 예외 클래스 | HTTP 상태 코드 | 전역 비즈니스 코드 (globalCode) | 용도 |
| :--- | :--- | :--- | :--- |
| `InvalidInputException` | 400 Bad Request | `GLOBAL-000` | 입력값 검증 실패, 잘못된 요청 |
| `UnauthorizedException` | 401 Unauthorized | `GLOBAL-100` | 인증 실패, 토큰 만료 |
| `ForbiddenException` | 403 Forbidden | `GLOBAL-200` | 권한 없음 |
| `NotFoundException` | 404 Not Found | `GLOBAL-300` | 리소스 존재하지 않음 |
| `ConflictException` | 409 Conflict | `GLOBAL-500` | 중복 데이터, 상태 충돌 |
| `UnprocessableEntityException` | 422 Unprocessable Entity | `GLOBAL-700` | 요청을 처리할 수 없음 |
| `InfraFailureException` | 500 Internal Error | `GLOBAL-900` | 외부 API(FCM, SMS), DB 장애 |
| `InternalServerException` | 500 Internal Error | `GLOBAL-901` | 기타 모든 서버 내부 오류 |

### **직접 예외를 던지는 경우 (예외적 상황)**
세부 비즈니스 코드가 필요 없는 범용적인 상황에서는 시맨틱 예외를 직접 던질 수 있습니다. 개발자는 예외 발생 시 **구체적인 원인(message)**과 **재현에 필요한 데이터(metadata)**를 함께 넘깁니다.

```kotlin
throw NotFoundException(
    message = "리소스가 존재하지 않습니다.",
    metadata = mapOf("id" to id)
)
```

---

## 4. 로깅 및 장애 진단 (Logging & Observability)

### **Loki 로깅 전략**
*   모든 예외는 `BaseExceptionHandler`에서 가로채어 로그를 남깁니다.
*   로그에는 `traceId`, `uri`, `method`, `message`, `data`가 포함되어 Loki에서 즉시 쿼리가 가능합니다.
*   **Critical 에러(`isCritical()`가 true인 경우)**: `INFRA_FAILURE`나 `INTERNAL_SERVER_ERROR` 발생 시 자동으로 디스코드 알림이 발송됩니다.

### **Trace ID 활용**
모든 에러 응답의 로그에는 `traceId`가 포함되어 있습니다. 사용자가 에러를 제보할 때 로그상의 `traceId`를 확인하면 해당 요청의 전체 흐름(비동기 작업 포함)을 추적할 수 있습니다.
