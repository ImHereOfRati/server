package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : BaseErrorCode {
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "USER_001",
        "사용자를 찾을 수 없습니다."
    ),
    USER_NOT_ACTIVE(
        HttpStatus.NOT_FOUND,
        "USER_002",
        "사용자가 PENDING 상태입니다."
    ),
    USER_ID_NULL(
        HttpStatus.BAD_REQUEST,
        "USER_003",
        "사용자의 ID가 없습니다."
    ),
    FCM_TOKEN_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "FCM_001",
        "사용자의 FCM 토큰을 찾을 수 없습니다"
    ),
}
