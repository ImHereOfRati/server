package com.kdongsu5509.user.adapter.out.auth

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import com.kdongsu5509.user.application.port.out.user.JwtVerificationPort
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
class JjwtVerifyAdapter(
    private val kakaoOIDCProperties: KakaoOIDCProperties,
) : JwtVerificationPort {

    override fun verifyPayLoad(payload: OIDCDecodePayload) {
        verifyIssuer(payload.iss)
        verifyAudience(payload.aud)
    }

    override fun verifySignature(
        token: String,
        modulus: String,
        exponent: String
    ): Jws<Claims> {
        return try {
            val publicKey = createRSAPublicKey(modulus, exponent)
            Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
        } catch (e: ExpiredJwtException) {
            throw BusinessException(AuthErrorCode.OIDC_EXPIRED)
        }
    }

    private fun verifyIssuer(actualIssuer: String) {
        if (actualIssuer != kakaoOIDCProperties.issuer) {
            throw BusinessException(
                AuthErrorCode.OIDC_INVALID,
                "토큰의 issuer가 일치하지 않습니다. 토큰 Issuer: $actualIssuer"
            )
        }
    }

    private fun verifyAudience(actualAudience: String) {
        if (actualAudience != kakaoOIDCProperties.audience) {
            throw BusinessException(
                AuthErrorCode.OIDC_INVALID,
                "토큰의 audience가 일치하지 않습니다. 토큰 Audience: $actualAudience"
            )
        }
    }

    private fun createRSAPublicKey(modulus: String, exponent: String): Key {
        try {
            return createKey(modulus, exponent)
        } catch (e: NoSuchAlgorithmException) {
            throw BusinessException(AuthErrorCode.ALGORITHM_NOT_FOUND)
        } catch (e: InvalidKeySpecException) {
            throw BusinessException(AuthErrorCode.INVALID_KEY)
        } catch (e: IllegalArgumentException) {
            throw BusinessException(AuthErrorCode.INVALID_ENCODING)
        }
    }

    private fun createKey(modulus: String, exponent: String): Key {
        val keyFactory = KeyFactory.getInstance("RSA")
        val decodeN = Base64.getUrlDecoder().decode(modulus)
        val decodeE = Base64.getUrlDecoder().decode(exponent)
        val n = BigInteger(1, decodeN)
        val e = BigInteger(1, decodeE)

        val keySpec = RSAPublicKeySpec(n, e)
        return keyFactory.generatePublic(keySpec)
    }
}