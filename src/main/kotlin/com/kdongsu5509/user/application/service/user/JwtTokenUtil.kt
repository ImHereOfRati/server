package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenUtil(private val jwtProperties: JwtProperties) {

    private val zoneID: ZoneId = ZoneId.systemDefault()
    private val secretKey: SecretKey by lazy {
        val keyBytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        print(JwtTokenUtil::class.simpleName + " 의 비밀 키 : " + jwtProperties.secret)
        Keys.hmacShaKeyFor(keyBytes)
    }

    fun validateToken(token: String): Boolean {
        try {
            parseClaims(token)
            return true
        } catch (e: Exception) {
            val errorCode = when (e) {
                is ExpiredJwtException -> AuthErrorCode.IMHERE_EXPIRED_TOKEN
                else -> AuthErrorCode.IMHERE_INVALID_TOKEN
            }

            throw BusinessException(errorCode)
        }
    }

    fun getJwtIdFromToken(token: String): String {
        return parseClaims(token).id
    }

    fun getUIDFromToken(token: String): UUID {
        val uidValue = parseClaims(token)[JwtClaimKeys.CLAIM_USER_ID] as String
        return UUID.fromString(uidValue)
    }

    fun getUserEmailFromToken(token: String): String {
        return parseClaims(token)[JwtClaimKeys.CLAIM_EMAIL] as String
    }

    fun getUserNicknameFromToken(token: String): String {
        return parseClaims(token)[JwtClaimKeys.CLAIM_NICKNAME] as String
    }

    fun getRoleFromToken(token: String): String {
        val roleInfo = parseClaims(token)[JwtClaimKeys.CLAIM_ROLE] as String
        return roleInfo.removePrefix("ROLE_")
    }

    fun getStatusFromToken(token: String): String {
        return parseClaims(token)[JwtClaimKeys.CLAIM_STATUS] as String
    }

    fun getExpirationDateFromToken(token: String): LocalDateTime {
        val expiration = parseClaims(token).expiration
        return LocalDateTime.ofInstant(expiration.toInstant(), zoneID)
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .body
    }
}
