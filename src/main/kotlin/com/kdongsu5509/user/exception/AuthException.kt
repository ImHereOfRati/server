package com.kdongsu5509.user.exception

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
    INVALID_KEY(CommonErrorCode.INVALID_INPUT, "AUTH-001", "검증에 실패한 공개키입니다."),
    INVALID_ENCODING(CommonErrorCode.INVALID_INPUT, "AUTH-002", "잘못된 Base64 인코딩 값입니다."),
    UNSUPPORTED_SOCIAL_TYPE(CommonErrorCode.INVALID_INPUT, "AUTH-003", "지원하지 않는 소셜 로그인 타입입니다."),

    // --- 1xx: Unauthorized (401) ---
    OIDC_INVALID(CommonErrorCode.UNAUTHORIZED, "AUTH-100", "OIDC ID 토큰 검증에 실패했습니다."),
    OIDC_EXPIRED(CommonErrorCode.UNAUTHORIZED, "AUTH-101", "OIDC ID 토큰이 만료되었습니다."),
    IMHERE_EXPIRED_TOKEN(CommonErrorCode.UNAUTHORIZED, "TOKEN-100", "만료된 토큰입니다."),
    IMHERE_INVALID_TOKEN(CommonErrorCode.UNAUTHORIZED, "TOKEN-101", "유효하지 않은 토큰입니다."),
    IMHERE_KEY_MISMATCH(CommonErrorCode.UNAUTHORIZED, "TOKEN-102", "토큰과 일치하지 않는 키 정보입니다."),
    IMHERE_KEY_NOT_FOUND_IN_REDIS(CommonErrorCode.UNAUTHORIZED, "TOKEN-103", "인증 정보를 찾을 수 없거나 만료되었습니다."),

    // --- 2xx: Forbidden (403) ---
    IMHERE_ACCESS_DENIED(CommonErrorCode.FORBIDDEN, "AUTH-200", "해당 기능에 대한 권한이 없습니다."),

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
    SOCIAL_LOGIN_COMMUNICATION_ERROR(CommonErrorCode.INFRA_FAILURE, "AUTH-903", "소셜 로그인 서버와의 통신 중 오류가 발생했습니다."),
    OIDC_KEY_PARSING_ERROR(CommonErrorCode.INTERNAL_SERVER_ERROR, "AUTH-904", "OIDC 키 파싱 중 오류가 발생했습니다");

    override val httpStatus: HttpStatus = category.httpStatus
}
