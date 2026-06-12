package com.kdongsu5509.auth

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    ALGORITHM_NOT_FOUND(CommonErrorCode.INVALID_INPUT, "AUTH-000", "공개키 알고리즘을 찾을 수 없습니다."),
    INVALID_ENCODING(CommonErrorCode.INVALID_INPUT, "AUTH-001", "잘못된 Base64 인코딩 값입니다."),
    MISSING_REQUEST_HEADER(CommonErrorCode.INVALID_INPUT, "AUTH-002", "필수 헤더 값이 누락되었습니다."),

    // --- 1xx: Unauthorized (401) ---
    OIDC_EXPIRED(CommonErrorCode.UNAUTHORIZED, "AUTH-100", "OIDC ID 토큰이 만료되었습니다."),
    OIDC_FORMAT_INVALID(CommonErrorCode.UNAUTHORIZED, "AUTH-101", "OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다."),
    OIDC_SIGNATURE_INVALID(CommonErrorCode.UNAUTHORIZED, "AUTH-102", "OIDC ID 토큰의 서명 검증에 실패했습니다."),
    OIDC_MISSING_EMAIL(CommonErrorCode.UNAUTHORIZED, "AUTH-103", "OIDC ID 토큰에 필수 정보(이메일)가 없습니다."),
    INVALID_OTT(CommonErrorCode.UNAUTHORIZED, "AUTH-104", "유효하지 않은 일회용 토큰입니다."),
    USER_DISABLED(CommonErrorCode.UNAUTHORIZED, "AUTH-105", "비활성화된 계정입니다."),
    USER_LOCKED(CommonErrorCode.UNAUTHORIZED, "AUTH-106", "잠긴 계정입니다."),
    USER_PENDING(CommonErrorCode.UNAUTHORIZED, "AUTH-107", "가입 대기 중인 계정입니다. 이메일 인증 또는 약관 동의가 필요합니다."),
    USER_WITHDRAWN(CommonErrorCode.UNAUTHORIZED, "AUTH-108", "탈퇴한 계정입니다."),
    IMHERE_EXPIRED_TOKEN(CommonErrorCode.UNAUTHORIZED, "TOKEN-100", "만료된 토큰입니다."),
    IMHERE_INVALID_TOKEN(CommonErrorCode.UNAUTHORIZED, "TOKEN-101", "유효하지 않은 토큰입니다."),
    IMHERE_INVALID_TOKEN_SIG(CommonErrorCode.UNAUTHORIZED, "TOKEN-102", "토큰의 서명 정보가 일치하지 않습니다."),
    IMHERE_KEY_NOT_FOUND_IN_REDIS(CommonErrorCode.UNAUTHORIZED, "TOKEN-103", "인증 정보를 찾을 수 없거나 만료되었습니다."),

    // --- 2xx: Forbidden (403) ---
    IMHERE_ACCESS_DENIED(CommonErrorCode.FORBIDDEN, "AUTH-200", "해당 기능에 대한 권한이 없습니다."),
    IMHERE_ALREADY_ACTIVE(CommonErrorCode.FORBIDDEN, "AUTH-201", "이미 활성화된 계정입니다. 해당 요청을 처리할 권한이 없습니다"),

    // --- 3xx : NotFound (404) ---
    USER_NOT_REGISTER(CommonErrorCode.NOT_FOUND, "AUTH-300", "사용자 정보를 찾을 수 없습니다."),

    // --- 9xx: Internal Error (500) ---
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED(
        CommonErrorCode.INTERNAL_SERVER_ERROR,
        "AUTH-900",
        "카카오 서버로부터 공개키를 가져오는데 실패했습니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED(
        CommonErrorCode.INFRA_FAILURE,
        "AUTH-901",
        "Redis로부터 공개키를 가져오는데 실패했습니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND(CommonErrorCode.INFRA_FAILURE, "AUTH-902", "공개키 목록에서 일치하는 키를 찾을 수 없습니다."),
    OIDC_KEY_PARSING_ERROR(CommonErrorCode.INTERNAL_SERVER_ERROR, "AUTH-903", "OIDC 키 파싱 중 오류가 발생했습니다"),
    IMHERE_KEY_EXCEPTION(CommonErrorCode.INTERNAL_SERVER_ERROR, "AUTH-904", "토큰 검증 중 오류가 발생하였습니다. 관리자에게 문의해주세요");

    override val httpStatus: HttpStatus = category.httpStatus
}
