package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : BaseErrorCode {
    OIDC_INVALID(
        HttpStatus.BAD_REQUEST,
        "AUTH_COMMON_001",
        "OIDC ID 토큰이 유효하지 않습니다."
    ),
    OIDC_EXPIRED(
        HttpStatus.BAD_REQUEST,
        "AUTH_COMMON_002",
        "OIDC ID 토큰이 만료되었습니다."
    ),
    ALGORITHM_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "AUTH_COMMON_003",
        "암호화 알고리즘을 찾을 수 없습니다."
    ),
    INVALID_KEY(
        HttpStatus.BAD_REQUEST,
        "AUTH_COMMON_004",
        "유효하지 않은 공개키 스펙입니다."
    ),
    INVALID_ENCODING(
        HttpStatus.BAD_REQUEST,
        "AUTH_COMMON_005",
        "잘못된 Base64 인코딩 값입니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "AUTH_KAKAO_001",
        "카카오 서버에서 OIDC 공개 키를 가져오는데 실패했습니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "AUTH_KAKAO_002",
        "Redis에서 OIDC 공개 키를 가져오는데 실패했습니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "AUTH_KAKAO_003",
        "OIDC 공개 키 목록에서 공개키를 가져오는데 실패했습니다."
    ),
    IMHERE_EXPIRED_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "IMHERE_TOKEN_001",
        "만료된 토큰입니다"
    ),
    IMHERE_INVALID_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "IMHERE_TOKEN_002",
        "잘못된 토큰입니다"
    ),
}
