package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.BusinessCode
import com.kdongsu5509.support.exception.ErrorReason

/**
 * 사용자 도메인 관련 비즈니스 에러 (USER)
 *
 * [번호 체계 가이드]
 * - 0xx: 400 Bad Request (입력값 검증 오류 등)
 * - 1xx: 404 Not Found (사용자 부재 등)
 * - 2xx: 409 Conflict (데이터 중복 등)
 */
enum class UserError(
    override val errorCategory: ErrorReason,
    override val businessCode: String,
    override val message: String? = null
) : BusinessCode {
    // --- 0xx: Bad Request (400) ---
    USER_ID_NULL(ErrorReason.INVALID_INPUT, "USER_001", "사용자 식별값이 없습니다."),
    INVALID_USER_STATUS(ErrorReason.INVALID_INPUT, "USER_002", "유효하지 않은 사용자 상태입니다."),

    // --- 1xx: Resource Absence (404) ---
    USER_NOT_FOUND(ErrorReason.NOT_FOUND, "USER_101", "사용자를 찾을 수 없습니다."),
    USER_NOT_ACTIVE(ErrorReason.NOT_FOUND, "USER_102", "비활성화된 사용자입니다."),

    // --- 2xx: State Conflict (409) ---
    DUPLICATE_EMAIL(ErrorReason.CONFLICT, "USER_201", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(ErrorReason.CONFLICT, "USER_202", "이미 사용 중인 닉네임입니다.")
}
