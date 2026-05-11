package com.kdongsu5509.user.adapter.out.auth.jwt

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import com.kdongsu5509.user.application.port.out.user.OIDCIdTokenVerifyPort
import com.kdongsu5509.user.exception.AuthError
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

/**
 * JJWT 라이브러리를 사용하여 OIDC ID 토큰의 유효성을 검증하는 어댑터입니다.
 *
 * 이 클래스는 다음과 같은 핵심 역할을 수행합니다:
 * 1. [getKid]: 서명 검증 전, 토큰 헤더에서 'kid'(Key ID)를 추출하여 적절한 공개키를 찾을 수 있게 합니다.
 * 2. [verifySignature]: 외부 제공자(예: 카카오)의 RSA 공개키를 사용하여 토큰의 서명 위변조 여부를 물리적으로 검증합니다.
 * 3. [verifyPayLoad]: 서명이 확인된 토큰 내부의 발급자(iss) 및 대상자(aud)가 우리 서비스 설정과 일치하는지 논리적으로 검증합니다.
 */
@Component
class JjwtOIDCIdTokenVerifyAdapter(
    private val kakaoOIDCProperties: KakaoOIDCProperties,
) : OIDCIdTokenVerifyPort {

    companion object {
        private val KID_PATTERN = "\"kid\"\\s*:\\s*\"([^\"]+)\"".toRegex()
    }

    override fun getKid(token: String): String {
        val splitToken = token.split(".")
        if (splitToken.size < 2) {
            AuthError.OIDC_INVALID.throwIt(customMessage = "잘못된 토큰 형식입니다.")
        }
        val header = String(Base64.getUrlDecoder().decode(splitToken[0]))

        val kidMatch = KID_PATTERN.find(header)
        return kidMatch?.groupValues?.get(1)
            ?: AuthError.OIDC_INVALID.throwIt(customMessage = "ID 토큰 헤더에 kid가 없습니다.")
    }

    override fun verifyPayLoad(payload: OIDCDecodePayload) {
        verifyIssuer(payload.iss)
        verifyAudience(payload.aud)
    }

    override fun verifySignature(
        token: String,
        modulus: String,
        exponent: String
    ): Jws<Claims> {
        val publicKey = createRSAPublicKey(modulus, exponent)

        return runCatching {
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
        }.getOrElse { e ->
            when (e) {
                is ExpiredJwtException -> AuthError.OIDC_EXPIRED.throwIt(cause = e)
                else -> AuthError.OIDC_INVALID.throwIt(cause = e)
            }
        }
    }

    private fun verifyIssuer(actualIssuer: String) {
        if (actualIssuer != kakaoOIDCProperties.issuer) {
            AuthError.OIDC_INVALID.throwIt(customMessage = "토큰의 issuer가 일치하지 않습니다. (Issuer: $actualIssuer)")
        }
    }

    private fun verifyAudience(actualAudience: String) {
        if (actualAudience != kakaoOIDCProperties.audience) {
            AuthError.OIDC_INVALID.throwIt(customMessage = "토큰의 audience가 일치하지 않습니다. (Audience: $actualAudience)")
        }
    }

    private fun createRSAPublicKey(modulus: String, exponent: String): PublicKey {
        return try {
            val n = BigInteger(1, Base64.getUrlDecoder().decode(modulus))
            val e = BigInteger(1, Base64.getUrlDecoder().decode(exponent))
            val keySpec = RSAPublicKeySpec(n, e)

            KeyFactory.getInstance("RSA").generatePublic(keySpec)
        } catch (e: NoSuchAlgorithmException) {
            AuthError.ALGORITHM_NOT_FOUND.throwIt(cause = e)
        } catch (e: InvalidKeySpecException) {
            AuthError.OIDC_KEY_PARSING_ERROR.throwIt(cause = e)
        } catch (e: IllegalArgumentException) {
            AuthError.INVALID_ENCODING.throwIt(cause = e)
        }
    }
}
