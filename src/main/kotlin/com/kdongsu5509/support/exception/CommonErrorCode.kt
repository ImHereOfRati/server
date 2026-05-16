package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val httpStatus: HttpStatus,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    /** 입력값 검증 실패 (400) */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "GLOBAL-000", "잘못된 요청입니다. 입력값을 확인해주세요."),

    /** JSON 파싱 오류 등 읽을 수 없는 메시지 (400) */
    INVALID_HTTP_MESSAGE(HttpStatus.BAD_REQUEST, "GLOBAL-001", "요청 바디를 읽을 수 없거나 형식이 잘못되었습니다."),

    // --- 1xx: Unauthorized (401) ---
    /** 인증 실패 (401) */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL-100", "인증에 실패했습니다. 다시 로그인해주세요."),

    // --- 2xx: Forbidden (403) ---
    /** 권한 없음 (403) */
    FORBIDDEN(HttpStatus.FORBIDDEN, "GLOBAL-200", "해당 리소스에 접근할 권한이 없습니다."),

    // --- 3xx: Not Found (404) ---
    /** 리소스를 찾을 수 없음 (404) */
    NOT_FOUND(HttpStatus.NOT_FOUND, "GLOBAL-300", "요청하신 리소스를 찾을 수 없습니다."),

    // --- 4xx: Method Not Allowed (405) ---
    /** 잘못된 HTTP 메서드 (405) */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GLOBAL-400", "지원하지 않는 HTTP 메서드입니다."),

    // --- 5xx: Conflict (409) ---
    /** 중복 데이터 등 상태 충돌 (409) */
    CONFLICT(HttpStatus.CONFLICT, "GLOBAL-500", "이미 존재하는 데이터이거나 상태가 충돌합니다."),

    // --- 6xx: Unsupported Media Type (415) ---
    /** 지원하지 않는 미디어 타입 (415) */
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "GLOBAL-600", "지원하지 않는 미디어 타입입니다."),

    // --- 7xx: Unprocessable Entity (422) ---
    /** 처리할 수 없는 요청 (422) */
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "GLOBAL-700", "요청을 처리할 수 없습니다."),

    // --- 9xx: Internal Server Error (500) ---
    /** 외부 인프라 서비스 장애 (500) */
    INFRA_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-900", "외부 서비스와의 통신 중 오류가 발생했습니다."),

    /** 기타 서버 내부 오류 (500) */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL-901", "서버 내부 오류가 발생했습니다.");
}
