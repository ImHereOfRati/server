package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.KakaoOIDCProperties
import com.kdongsu5509.auth.application.OIDCDecodePayload
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.support.exception.throwIt
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
class JjwtOIDCTokenVerifyAdapter(
    private val kakaoOIDCProperties: KakaoOIDCProperties,
) : OIDCIdTokenVerifyPort {

    companion object {
        private val KID_PATTERN = """ "kid"\s*:\s*"([^"]+)" """.trim().toRegex()
    }

    override fun getKid(token: String): String {
        val splitToken = token.split(".")
        if (splitToken.size < 2) {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }
        val header = String(Base64.getUrlDecoder().decode(splitToken[0]))

        val kidMatch = KID_PATTERN.find(header)
        return kidMatch?.groupValues?.get(1) ?: AuthException.OIDC_FORMAT_INVALID.throwIt()
    }

    override fun verifyPayLoad(payload: OIDCDecodePayload) {
        verifyIssuer(payload.iss)
        verifyAudience(payload.aud)
    }

    override fun verifySignature(token: String, modulus: String, exponent: String): Jws<Claims> {
        val publicKey = createRSAPublicKey(modulus, exponent)

        return runCatching {
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
        }.getOrElse { e ->
            when (e) {
                is ExpiredJwtException -> AuthException.OIDC_EXPIRED.throwIt(cause = e)
                else -> AuthException.OIDC_SIGNATURE_INVALID.throwIt(cause = e)
            }
        }
    }

    private fun verifyIssuer(actualIssuer: String) {
        if (actualIssuer != kakaoOIDCProperties.issuer) {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }
    }

    private fun verifyAudience(actualAudience: String) {
        if (actualAudience != kakaoOIDCProperties.audience) {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }
    }

    private fun createRSAPublicKey(modulus: String, exponent: String): PublicKey {
        return try {
            val n = BigInteger(1, Base64.getUrlDecoder().decode(modulus))
            val e = BigInteger(1, Base64.getUrlDecoder().decode(exponent))
            val keySpec = RSAPublicKeySpec(n, e)

            KeyFactory.getInstance("RSA").generatePublic(keySpec)
        } catch (e: NoSuchAlgorithmException) {
            AuthException.ALGORITHM_NOT_FOUND.throwIt(cause = e)
        } catch (e: InvalidKeySpecException) {
            AuthException.OIDC_KEY_PARSING_ERROR.throwIt(cause = e)
        } catch (e: IllegalArgumentException) {
            AuthException.INVALID_ENCODING.throwIt(cause = e)
        }
    }
}
