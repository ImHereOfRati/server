package com.kdongsu5509.auth.adapter.out.jwt

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.auth.application.service.dto.OIDCDecodePayload
import com.kdongsu5509.support.exception.throwIt
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import com.kdongsu5509.support.logger.logger
import java.math.BigInteger
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
class JjwtOIDCTokenVerifyAdapter : OIDCIdTokenVerifyPort {
    private val log = logger()

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

    override fun verifyPayLoad(
        payload: OIDCDecodePayload,
        allowedIssuers: Collection<String>,
        allowedAudiences: Collection<String>,
        nonce: String
    ) {
        verifyIssuer(payload.iss, allowedIssuers)
        verifyAudience(payload.audiences, allowedAudiences)
        verifyNonce(payload.nonce, nonce)
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

    private fun verifyIssuer(actualIssuer: String, allowedIssuers: Collection<String>) {
        if (allowedIssuers.isEmpty() || actualIssuer !in allowedIssuers) {
            log.warn("OIDC issuer 검증 실패: 실제 issuer={}, 허용 issuer={}", actualIssuer, allowedIssuers)
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }
    }

    private fun verifyAudience(actualAudiences: Collection<String>, allowedAudiences: Collection<String>) {
        if (actualAudiences.isEmpty() || allowedAudiences.none { it in actualAudiences }) {
            log.warn("OIDC audience 검증 실패: 실제 audience={}, 허용 audience={}", actualAudiences, allowedAudiences)
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }
    }

    private fun verifyNonce(actualNonce: String?, expectedNonce: String) {
        if (actualNonce.isNullOrBlank() || actualNonce != expectedNonce) {
            log.warn("OIDC nonce 검증 실패: 실제 nonce 존재={}, 기대 nonce 존재={}, 실제 길이={}, 기대 길이={}", !actualNonce.isNullOrBlank(), expectedNonce.isNotBlank(), actualNonce?.length ?: 0, expectedNonce.length)
            AuthException.OIDC_NONCE_INVALID.throwIt()
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
