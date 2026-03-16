package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class GlobalErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : BaseErrorCode {
    INVALID_INPUT_VALUE(
        HttpStatus.BAD_REQUEST,
        "INVALID_INPUT_VALUE",
        "요청 값에 오류가 있습니다"
    ),
    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "NO_PERMISSION",
        "권한이 없습니다"
    ),
    UNKNOWN_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "UNKNOWN_INTERNAL_SERVER",
        "알 수 없는 오류가 발생했습니다."
    ),
}
