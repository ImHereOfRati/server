package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class ImHereJjwtIssuerAdapter(
    private val imHereJwtProperties: ImHereJwtProperties,
    private val keyProvider: ImHereJjwtSecretKeyProvider
) : ImHereTokenIssuerPort {

    companion object {
        private val ZONE_ID = ZoneId.systemDefault()
    }

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

    private fun create(category: String, claims: JwtTokenClaims, expiredTime: LocalDateTime): String {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .claim(JwtClaimKeys.CLAIM_CATEGORY, category)
            .claim(JwtClaimKeys.CLAIM_USER_ID, claims.uid.toString())
            .claim(JwtClaimKeys.CLAIM_EMAIL, claims.email)
            .claim(JwtClaimKeys.CLAIM_NICKNAME, claims.nickname)
            .claim(JwtClaimKeys.CLAIM_ROLE, "ROLE_${claims.role}")
            .claim(JwtClaimKeys.CLAIM_STATUS, claims.status)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiredTime.atZone(ZONE_ID).toInstant()))
            .signWith(keyProvider.secretKey)
            .compact()
    }
}
