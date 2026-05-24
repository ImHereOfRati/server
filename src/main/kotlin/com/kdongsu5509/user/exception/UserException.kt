package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

enum class UserException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    USER_ID_NULL(CommonErrorCode.INVALID_INPUT, "USER-000", "사용자 식별값이 없습니다."),
    INVALID_USER_STATUS(CommonErrorCode.INVALID_INPUT, "USER-001", "유효하지 않은 사용자 상태입니다."),


    // --- 3xx: Resource Absence (404) ---
    USER_NOT_FOUND(CommonErrorCode.NOT_FOUND, "USER-300", "사용자를 찾을 수 없습니다."),
    USER_NOT_ACTIVE(CommonErrorCode.NOT_FOUND, "USER-301", "비활성화된 사용자입니다."),

    // --- 5xx: State Conflict (409) ---
    DUPLICATE_EMAIL(CommonErrorCode.CONFLICT, "USER-500", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(CommonErrorCode.CONFLICT, "USER-501", "이미 사용 중인 닉네임입니다.");

    override val httpStatus: HttpStatus = category.httpStatus
}
