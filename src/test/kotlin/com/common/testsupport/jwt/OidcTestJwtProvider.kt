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
    const val PAYLOAD_AUD = "test-kakao-client-id"
    const val GOOGLE_PAYLOAD_ISS = "https://accounts.google.com"
    const val GOOGLE_PAYLOAD_AUD = "test-google-client-id"
    const val PAYLOAD_SUB = "사용자회원번호"
    const val PAYLOAD_EMAIL = "ds.ko@kakao.com"
    const val PAYLOAD_EXP_SECONDS = 3600L

    val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair()

    val testPublicKey = keyPair.public
    val testPrivateKey = keyPair.private

    fun buildIdToken(
        email: String = PAYLOAD_EMAIL,
        issuer: String = PAYLOAD_ISS,
        audience: String = PAYLOAD_AUD,
        nonce: String = UUID.randomUUID().toString(),
        expiresInSeconds: Long = PAYLOAD_EXP_SECONDS
    ): String {
        val now = Instant.now()
        val issuedAt = Date.from(now)
        val expiration = Date.from(now.plusSeconds(expiresInSeconds))

        return Jwts.builder()
            .header()
            .add("typ", HEADER_TYP)
            .add("kid", HEADER_KID)
            .add("alg", HEADER_ALG)
            .and()
            .issuer(issuer)
            .audience().add(audience).and()
            .subject(PAYLOAD_SUB)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .claim("auth_time", issuedAt)
            .claim("nonce", nonce)
            .claim("email", email)
            .signWith(testPrivateKey, Jwts.SIG.RS256)
            .compact()
    }

    fun buildGoogleIdToken(email: String = "google@example.com"): String = buildIdToken(
        email = email,
        issuer = GOOGLE_PAYLOAD_ISS,
        audience = GOOGLE_PAYLOAD_AUD
    )
}
