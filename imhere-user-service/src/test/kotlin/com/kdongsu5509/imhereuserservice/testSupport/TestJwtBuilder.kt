package com.kdongsu5509.imhereuserservice.testSupport

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.*

object TestJwtBuilder {

    const val KAKAO_HEADER_ALG = "RS256"
    const val KAKAO_HEADER_TYP = "JWT"
    const val KAKAO_HEADER_KID = "test-kid"

    const val KAKAO_PAYLOAD_ISS = "https://kauth.kakao.com"
    const val KAKAO_PAYLOAD_AUD = "bf284f33bfeba9bc59575706d0eb0e9c" // 테스트용 앱 키
    const val KAKAO_PAYLOAD_SUB = "사용자회원번호"
    const val KAKAO_PAYLOAD_EMAIL = "ds.ko@kakao.com"
    const val KAKAO_PAYLOAD_EXP_SECONDS = 3600L

    val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)  // 최소 2048비트 RSA 키 필요
    }.generateKeyPair()
    val testPublicKey = keyPair.public
    val testPrivateKey = keyPair.private


    /**
     * 유효한 카카오 규격의 ID 토큰을 생성하는 빌더 메서드입니다.
     */
    fun buildValidIdToken(): String {
        return createToken(KAKAO_PAYLOAD_EMAIL)
    }

    fun buildValidIdTokenWithCustomEmail(email: String): String {
        return createToken(email)
    }

    private fun createToken(email: String): String {
        val now = Instant.now()
        val issuedAt = Date.from(now)
        val expiration = Date.from(now.plusSeconds(KAKAO_PAYLOAD_EXP_SECONDS))

        val payload: Map<String, Any> = mapOf(
            "iss" to KAKAO_PAYLOAD_ISS,
            "aud" to KAKAO_PAYLOAD_AUD,
            "sub" to KAKAO_PAYLOAD_SUB,
            "iat" to issuedAt,
            "exp" to expiration,
            "auth_time" to issuedAt,
            "nonce" to UUID.randomUUID().toString(),
            "email" to email
        )

        val testJwt = Jwts.builder()
            .setHeaderParams(
                mapOf(
                    "typ" to KAKAO_HEADER_TYP,
                    "kid" to KAKAO_HEADER_KID,
                    "alg" to KAKAO_HEADER_ALG
                )
            )
            .setClaims(payload)
            .signWith(testPrivateKey, SignatureAlgorithm.RS256)
            .compact()

        println("TEST JWT for $email : $testJwt")
        return testJwt
    }
}