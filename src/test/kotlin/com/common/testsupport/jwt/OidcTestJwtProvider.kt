package com.common.testsupport.jwt

import io.jsonwebtoken.Jwts
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.*

object OidcTestJwtProvider {

    const val HEADER_ALG = "RS256"
    const val HEADER_TYP = "JWT"
    const val HEADER_KID = "test-kid"

    const val PAYLOAD_ISS = "https://kauth.kakao.com"
    const val PAYLOAD_AUD = "bf284f33bfeba9bc59575706d0eb0e9c" // 테스트용 AUD
    const val PAYLOAD_SUB = "사용자회원번호"
    const val PAYLOAD_EMAIL = "ds.ko@kakao.com"
    const val PAYLOAD_EXP_SECONDS = 3600L

    val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()

    val testPublicKey = keyPair.public
    val testPrivateKey = keyPair.private

    /**
     * 유효한 카카오 규격의 ID 토큰을 생성합니다.
     */
    fun buildIdToken(email: String = PAYLOAD_EMAIL): String {
        val now = Instant.now()
        val issuedAt = Date.from(now)
        val expiration = Date.from(now.plusSeconds(PAYLOAD_EXP_SECONDS))

        return Jwts.builder()
            .header()
            .add("typ", HEADER_TYP)
            .add("kid", HEADER_KID)
            .add("alg", HEADER_ALG)
            .and()
            .issuer(PAYLOAD_ISS)
            .audience().add(PAYLOAD_AUD).and()
            .subject(PAYLOAD_SUB)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .claim("auth_time", issuedAt)
            .claim("nonce", UUID.randomUUID().toString())
            .claim("email", email)
            .signWith(testPrivateKey, Jwts.SIG.RS256)
            .compact()
    }
}
