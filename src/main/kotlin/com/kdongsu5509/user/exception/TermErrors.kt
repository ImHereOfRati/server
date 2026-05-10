package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.BusinessCode
import com.kdongsu5509.support.exception.ErrorReason

/**
 * 약관 관련 비즈니스 에러 (TERM)
 *
 * [번호 체계 가이드]
 * - 0xx: 400 Bad Request (데이터 오류)
 * - 1xx: 404 Not Found (약관 부재)
 * - 2xx: 409 Conflict (중복 정의)
 */
enum class TermError(
    override val errorCategory: ErrorReason,
    override val businessCode: String,
    override val message: String? = null
) : BusinessCode {
    // --- 0xx: Bad Request (400) ---
    OBLIGATORY_TERM_NOT_AGREED(ErrorReason.INVALID_INPUT, "TERM_001", "필수 약관에 동의해야 합니다."),

    // --- 1xx: Resource Absence (404) ---
    TERM_DEFINITION_NOT_FOUND(ErrorReason.NOT_FOUND, "TERM_101", "해당 약관 정의를 찾을 수 없습니다."),

    // --- 2xx: State Conflict (409) ---
    TERM_DEFINITION_ALREADY_EXIST(ErrorReason.CONFLICT, "TERM_201", "이미 존재하는 약관 정의입니다.")
}
