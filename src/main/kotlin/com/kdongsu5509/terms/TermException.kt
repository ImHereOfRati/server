package com.kdongsu5509.terms

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

enum class TermException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    OBLIGATORY_TERM_NOT_AGREED(CommonErrorCode.INVALID_INPUT, "TERM-000", "필수 약관에 동의해야 합니다."),

    // --- 3xx: Resource Absence (404) ---
    TERM_NOT_FOUND(CommonErrorCode.NOT_FOUND, "TERM-300", "해당 약관 정의를 찾을 수 없습니다."),

    // --- 5xx: State Conflict (409) ---
    TERM_DEFINITION_ALREADY_EXIST(CommonErrorCode.CONFLICT, "TERM-500", "이미 존재하는 약관 정의입니다."),

    // --- 7xx: Unprocessable Entity (422) ---
    NON_ACTIVE_TERM_NOT_ALLOWED(CommonErrorCode.UNPROCESSABLE_ENTITY, "TERM-700", "비활성화된 약관은 조회할 수 없습니다.");

    override val httpStatus: HttpStatus = category.httpStatus
}
