package com.kdongsu5509.user.adapter.out.auth.jwt

import com.kdongsu5509.user.application.dto.JwtTokenClaims
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenIssuerPort
import com.kdongsu5509.user.application.service.user.auth.JwtClaimKeys
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 우리 서비스 전용 자체 JWT(Access, Refresh, Admin) 토큰을 생성하는 어댑터입니다.
 *
 * JJWT 라이브러리를 사용하여 유저 정보를 기반으로 암호화된 토큰을 발행하며,
 * [ImHereJwtProperties]에 정의된 각 토큰별 만료 시간을 적용합니다.
 */
@Component
class ImHereJjwtIssuerAdapter(
    private val imHereJwtProperties: ImHereJwtProperties,
    private val keyProvider: ImHereJjwtKeyProvider
) : ImHereTokenIssuerPort {

    private val zoneID: ZoneId = ZoneId.systemDefault()

    override fun createAccessToken(claims: JwtTokenClaims): String {
        val expiredTime = LocalDateTime.now().plusMinutes(imHereJwtProperties.accessExpirationMinutes)
        return create(JwtClaimKeys.ACCESS_TOKEN, claims, expiredTime)
    }

    override fun createRefreshToken(claims: JwtTokenClaims): String {
        val expiredTime = LocalDateTime.now().plusDays(imHereJwtProperties.refreshExpirationDays)
        return create(JwtClaimKeys.REFRESH_TOKEN, claims, expiredTime)
    }

    override fun createAdminAccessToken(claims: JwtTokenClaims): String {
        val expiredTime = LocalDateTime.now().plusMinutes(imHereJwtProperties.adminExpirationMinutes)
        return create(JwtClaimKeys.ACCESS_TOKEN, claims, expiredTime)
    }

    private fun create(
        category: String,
        claims: JwtTokenClaims,
        expiredTime: LocalDateTime
    ): String {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .claim(JwtClaimKeys.CLAIM_CATEGORY, category)
            .claim(JwtClaimKeys.CLAIM_USER_ID, claims.uid.toString())
            .claim(JwtClaimKeys.CLAIM_EMAIL, claims.email)
            .claim(JwtClaimKeys.CLAIM_NICKNAME, claims.nickname)
            .claim(JwtClaimKeys.CLAIM_ROLE, "ROLE_${claims.role}")
            .claim(JwtClaimKeys.CLAIM_STATUS, claims.status)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiredTime.atZone(zoneID).toInstant()))
            .signWith(keyProvider.secretKey)
            .compact()
    }
}
