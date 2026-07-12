package com.kdongsu5509.auth.domain

/**
 * Spring Security 권한 표기("ROLE_" 접두) 변환의 단일 소재지.
 *
 * 기존에는 발급(ImHereJjwtIssuerAdapter) / 파싱(ImHereJjwtParserAdapter) /
 * UserDetails(ImHereUserDetails) 세 지점이 각자 "ROLE_" 문자열을 부착·제거해
 * 표현 규칙이 중복되어 있었다. wire 포맷(String)은 그대로 두고 규칙만 이곳으로 모은다.
 */
object RoleAuthority {

    private const val AUTHORITY_PREFIX = "ROLE_"

    fun toAuthority(role: String): String = "$AUTHORITY_PREFIX$role"

    fun fromAuthority(authority: String): String = authority.removePrefix(AUTHORITY_PREFIX)
}
