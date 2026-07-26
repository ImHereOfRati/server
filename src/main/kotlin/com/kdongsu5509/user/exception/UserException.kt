package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode

enum class UserException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    ONLY_PENDING_CAN_BE_ACTIVE_USER(CommonErrorCode.INVALID_INPUT, "USER-001", "PENDING 상태인 사용자만 활성화 가능합니다"),
    WITHDRAW_USER(CommonErrorCode.INVALID_INPUT, "USER-002", "탈퇴한 사용자에 대한 상태 변경은 허용되지 않습니다."),
    ONLY_ACTIVE_USER_CAN_BE_BLOCKED(CommonErrorCode.INVALID_INPUT, "USER-003", "ACTIVE 상태인 사용자만 차단할 수 있습니다."),
    ONLY_BLOCKED_USER_CAN_BE_UNBLOCKED(CommonErrorCode.INVALID_INPUT, "USER-004", "BLOCKED 상태인 사용자만 차단 해제할 수 있습니다."),
    ONLY_ACTIVE_OR_BLOCKED_USER_CAN_WITHDRAW(
        CommonErrorCode.INVALID_INPUT,
        "USER-005",
        "ACTIVE 또는 BLOCKED 상태인 사용자만 탈퇴할 수 있습니다."
    ),

    // --- 3xx: Resource Absence (404) ---
    USER_NOT_FOUND(CommonErrorCode.NOT_FOUND, "USER-300", "사용자를 찾을 수 없습니다."),

    // --- 5xx: State Conflict (409) ---
    DUPLICATE_EMAIL(CommonErrorCode.CONFLICT, "USER-500", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(CommonErrorCode.CONFLICT, "USER-501", "이미 사용 중인 닉네임입니다.");

    override val httpStatus = category.httpStatus
}
