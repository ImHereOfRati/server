package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.BusinessCode
import com.kdongsu5509.support.exception.ErrorReason

/**
 * 인증 관련 비즈니스 에러 (AUTH)
 *
 * [번호 체계 가이드]
 * - 0xx: 400 Bad Request (인증 데이터 형식 오류 등)
 * - 1xx: 401 Unauthorized / 403 Forbidden (토큰 만료, 권한 없음 등)
 * - 9xx: 500 Internal Server Error (외부 인증 서버 통신 오류 등)
 */
enum class AuthError(
    override val errorCategory: ErrorReason,
    override val businessCode: String,
    override val message: String? = null
) : BusinessCode {
    // --- 0xx: Bad Request (400) ---
    ALGORITHM_NOT_FOUND(ErrorReason.INVALID_INPUT, "AUTH_001", "공개키 알고리즘을 찾을 수 없습니다."),
    INVALID_KEY(ErrorReason.INVALID_INPUT, "AUTH_002", "검증에 실패한 공개키입니다."),
    INVALID_ENCODING(ErrorReason.INVALID_INPUT, "AUTH_003", "잘못된 Base64 인코딩 값입니다."),
    UNSUPPORTED_SOCIAL_TYPE(ErrorReason.INVALID_INPUT, "AUTH_004", "지원하지 않는 소셜 로그인 타입입니다."),

    // --- 1xx: Auth & Permission (401, 403) ---
    OIDC_INVALID(ErrorReason.UNAUTHORIZED, "AUTH_101", "OIDC ID 토큰 검증에 실패했습니다."),
    OIDC_EXPIRED(ErrorReason.UNAUTHORIZED, "AUTH_102", "OIDC ID 토큰이 만료되었습니다."),
    IMHERE_ACCESS_DENIED(ErrorReason.FORBIDDEN, "AUTH_103", "해당 기능에 대한 권한이 없습니다."),
    IMHERE_EXPIRED_TOKEN(ErrorReason.UNAUTHORIZED, "TOKEN_101", "만료된 토큰입니다."),
    IMHERE_INVALID_TOKEN(ErrorReason.UNAUTHORIZED, "TOKEN_102", "유효하지 않은 토큰입니다."),
    IMHERE_KEY_MISMATCH(ErrorReason.UNAUTHORIZED, "TOKEN_103", "토큰과 일치하지 않는 키 정보입니다."),
    IMHERE_KEY_NOT_FOUND_IN_REDIS(ErrorReason.UNAUTHORIZED, "TOKEN_104", "인증 정보를 찾을 수 없거나 만료되었습니다."),

    // --- 9xx: Internal Error (500) ---
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED(ErrorReason.INTERNAL_SERVER_ERROR, "AUTH_901", "카카오 서버로부터 공개키를 가져오는데 실패했습니다."),
    KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED(
        ErrorReason.INFRA_FAILURE,
        "AUTH_902",
        "Redis로부터 공개키를 가져오는데 실패했습니다."
    ),
    KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND(ErrorReason.INFRA_FAILURE, "AUTH_903", "공개키 목록에서 일치하는 키를 찾을 수 없습니다."),
    SOCIAL_LOGIN_COMMUNICATION_ERROR(ErrorReason.INFRA_FAILURE, "AUTH_904", "소셜 로그인 서버와의 통신 중 오류가 발생했습니다."),
    OIDC_KEY_PARSING_ERROR(ErrorReason.INTERNAL_SERVER_ERROR, "AUTH_905", "OIDC 키 파싱 중 오류가 발생했습니다")
}
