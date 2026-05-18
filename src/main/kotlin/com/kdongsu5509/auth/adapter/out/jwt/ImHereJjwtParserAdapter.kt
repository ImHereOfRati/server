package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.JwtTokenClaims
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.support.exception.throwIt
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

@Component
class ImHereJjwtParserAdapter(
    private val keyProvider: ImHereJjwtSecretKeyProvider
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
            AuthException.IMHERE_EXPIRED_TOKEN.throwIt(cause = e)
        } catch (e: MalformedJwtException) {
            AuthException.IMHERE_INVALID_TOKEN.throwIt(cause = e)
        } catch (e: io.jsonwebtoken.security.SignatureException) {
            AuthException.IMHERE_INVALID_TOKEN_SIG.throwIt(cause = e)
        } catch (e: Exception) {
            AuthException.IMHERE_KEY_EXCEPTION.throwIt(cause = e)
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

