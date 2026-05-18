package com.common.testsupport.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

object ImHereTestJwtProvider {

    // application-test.yml 의 jwt.secret 과 동일한 값
    const val TEST_IMHERE_JWT_SECRET = "testSecretKeyForJwtAuthenticationTesting12345678901234567890"

    private val imHereSecretKey by lazy {
        Keys.hmacShaKeyFor(TEST_IMHERE_JWT_SECRET.toByteArray(StandardCharsets.UTF_8))
    }

    fun buildAccessToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = createToken("access", email, nickname, role, status, uid, expirationSeconds = 1800L)

    fun buildRefreshToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = createToken("refresh", email, nickname, role, status, uid, expirationSeconds = 604800L)

    private fun createToken(
        category: String,
        email: String,
        nickname: String,
        role: String,
        status: String,
        uid: UUID,
        expirationSeconds: Long
    ): String {
        val now = Instant.now()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .claim("category", category)
            .claim("uid", uid.toString())
            .claim("email", email)
            .claim("nickname", nickname)
            .claim("role", "ROLE_$role")
            .claim("status", status)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(imHereSecretKey)
            .compact()
    }
}
