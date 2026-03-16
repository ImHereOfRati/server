package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class TermErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : BaseErrorCode {
    TERM_DEFINITION_ALREADY_EXIST(
        HttpStatus.BAD_REQUEST,
        "TERM_001",
        "이미 존재하는 약관 입니다."
    ),
    TERM_DEFINITION_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "TERM_002",
        "해당 약관 종류는 존재하지 않습니다"
    ),
    OBLIGATORY_TERM_NOT_AGREED(
        HttpStatus.BAD_REQUEST,
        "TERM_003",
        "필수 약관에 동의하지 않았습니다."
    ),
}
