package com.kdongsu5509.imhereuserservice.support.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    /**
     * 공통 Auth 문제
     */
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

    /**
     * 공통 인증(RSA, 키 스펙) 관련 문제
     */
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

    /**
     * KAKAO OIDC ERRORS : 카카오 OIDC 관련 오류
     */
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

    /**
     * 자체 발급한 JWT 관련 오류
     */
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


    /**
     * USER ERRORS : 사용자 관련 오류
     */
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

    /**
     * TERM ERROR : 약관 관련 오류
     */
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

    /**
     * FCM ERRORS : FCM 토큰 관련 오류
     */
    FCM_TOKEN_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "FCM_001",
        "사용자의 FCM 토큰을 찾을 수 없습니다"
    ),

    /**
     * AUTHORIZATION : 권한 오류
     */
    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "NO_PERMISSION",
        "권한이 없습니다"
    ),


    /**
     * INTERNAL SERVER ERRORS : 알 수 없는 서버 오류
     */
    UNKNOWN_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "UNKNOWN_INTERNAL_SERVER",
        "알 수 없는 오류가 발생했습니다."
    ),
}