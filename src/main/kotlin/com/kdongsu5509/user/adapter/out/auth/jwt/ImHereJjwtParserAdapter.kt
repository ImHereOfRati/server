package com.kdongsu5509.user.adapter.out.auth.jwt

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.dto.JwtTokenClaims
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenParserPort
import com.kdongsu5509.user.application.service.user.auth.JwtClaimKeys
import com.kdongsu5509.user.exception.AuthError
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 우리 서비스 전용 자체 JWT 토큰을 파싱하고 유효성을 검증하는 어댑터입니다.
 *
 * 전달받은 토큰이 우리 서버의 SecretKey로 서명된 것이 맞는지 확인하고,
 * 토큰 내부의 클레임 데이터를 [JwtTokenClaims] 객체로 변환하여 반환합니다.
 */
@Component
class ImHereJjwtParserAdapter(
    private val keyProvider: ImHereJjwtKeyProvider
) : ImHereTokenParserPort {

    private val zoneID: ZoneId = ZoneId.systemDefault()

    override fun parse(token: String): JwtTokenClaims {
        val claims = parseClaims(token)
        return JwtTokenClaims(
            uid = UUID.fromString(claims[JwtClaimKeys.CLAIM_USER_ID] as String),
            email = claims[JwtClaimKeys.CLAIM_EMAIL] as String,
            nickname = claims[JwtClaimKeys.CLAIM_NICKNAME] as String,
            role = (claims[JwtClaimKeys.CLAIM_ROLE] as String).removePrefix("ROLE_"),
            status = claims[JwtClaimKeys.CLAIM_STATUS] as String,
            expiration = LocalDateTime.ofInstant(claims.expiration.toInstant(), zoneID)
        )
    }

    override fun validate(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: ExpiredJwtException) {
            AuthError.IMHERE_EXPIRED_TOKEN.throwIt(cause = e)
        } catch (e: Exception) {
            AuthError.IMHERE_INVALID_TOKEN.throwIt(cause = e)
        }
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(keyProvider.secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
