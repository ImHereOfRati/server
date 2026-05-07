package com.kdongsu5509.user.application.service.user

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class JwtTokenIssuerTest {

    private lateinit var jwtTokenIssuer: JwtTokenIssuer
    private lateinit var jwtProperties: JwtProperties

    companion object {
        val test_uid: UUID? = UUID.randomUUID()
        const val TEST_EMAIL = "rati@kakao.com"
        const val TEST_NICKNAME = "rati"
        const val TEST_ROLE = "NORMAL"
        const val TEST_STATUS = "ACTIVE"

        val imHereJwtTokenElements: ImHereJwtTokenElements = ImHereJwtTokenElements(
            uid = test_uid!!,
            userEmail = TEST_EMAIL,
            userNickname = TEST_NICKNAME,
            role = TEST_ROLE,
            status = TEST_STATUS
        )
    }

    @BeforeEach
    fun setUp() {
        jwtProperties = JwtProperties(
            secret = "testSecretKeyForJwtTokenIssuerTest12345678901234567890",
            accessExpirationMinutes = 30,
            refreshExpirationDays = 7
        )
        jwtTokenIssuer = JwtTokenIssuer(jwtProperties)
    }

    @Test
    @DisplayName("AccessToken을 성공적으로 생성한다")
    fun createAccessToken_success() {
        // when
        val accessToken = jwtTokenIssuer.createAccessToken(imHereJwtTokenElements)

        // then
        assertThat(accessToken).isNotEmpty()

        // 토큰 파싱하여 검증
        val secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload

        assertThat(claims[JwtClaimKeys.CLAIM_EMAIL]).isEqualTo(TEST_EMAIL)
        assertThat(claims[JwtClaimKeys.CLAIM_NICKNAME]).isEqualTo(TEST_NICKNAME)
        assertThat(claims[JwtClaimKeys.CLAIM_ROLE]).isEqualTo("ROLE_$TEST_ROLE")
        assertThat(claims[JwtClaimKeys.CLAIM_CATEGORY]).isEqualTo(JwtClaimKeys.ACCESS_TOKEN)
        assertThat(claims.id).isNotNull()
        assertThat(claims.issuedAt).isNotNull()
        assertThat(claims.expiration).isNotNull()

        // 만료 시간이 현재 시간보다 미래인지 확인
        val expirationTime = LocalDateTime.ofInstant(claims.expiration.toInstant(), ZoneId.systemDefault())
        assertThat(expirationTime).isAfter(LocalDateTime.now())
    }

    @Test
    @DisplayName("RefreshToken을 성공적으로 생성한다")
    fun createRefreshToken_success() {
        // when
        val refreshToken = jwtTokenIssuer.createRefreshToken(imHereJwtTokenElements)

        // then
        assertThat(refreshToken).isNotEmpty()

        // 토큰 파싱하여 검증
        val secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(refreshToken)
            .payload

        assertThat(claims[JwtClaimKeys.CLAIM_EMAIL]).isEqualTo(TEST_EMAIL)
        assertThat(claims[JwtClaimKeys.CLAIM_NICKNAME]).isEqualTo(TEST_NICKNAME)
        assertThat(claims[JwtClaimKeys.CLAIM_ROLE]).isEqualTo("ROLE_$TEST_ROLE")
        assertThat(claims[JwtClaimKeys.CLAIM_CATEGORY]).isEqualTo(JwtClaimKeys.REFRESH_TOKEN)
        assertThat(claims.id).isNotNull()
        assertThat(claims.issuedAt).isNotNull()
        assertThat(claims.expiration).isNotNull()

        // 만료 시간이 현재 시간보다 미래인지 확인
        val expirationTime = LocalDateTime.ofInstant(claims.expiration.toInstant(), ZoneId.systemDefault())
        assertThat(expirationTime).isAfter(LocalDateTime.now())
    }

    @Test
    @DisplayName("AccessToken과 RefreshToken의 만료 시간이 다르다")
    fun accessTokenAndRefreshTokenHaveDifferentExpiration() {
        // when
        val accessToken = jwtTokenIssuer.createAccessToken(imHereJwtTokenElements)
        val refreshToken = jwtTokenIssuer.createRefreshToken(imHereJwtTokenElements)

        // then
        val secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))

        val accessClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload

        val refreshClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(refreshToken)
            .payload

        val accessExpiration = LocalDateTime.ofInstant(accessClaims.expiration.toInstant(), ZoneId.systemDefault())
        val refreshExpiration = LocalDateTime.ofInstant(refreshClaims.expiration.toInstant(), ZoneId.systemDefault())

        // RefreshToken이 AccessToken보다 더 오래 유효해야 함
        assertThat(refreshExpiration).isAfter(accessExpiration)
    }

    @Test
    @DisplayName("같은 사용자로 생성한 토큰도 매번 다른 ID를 가진다")
    fun tokensHaveDifferentIds() {
        // when
        val token1 = jwtTokenIssuer.createAccessToken(imHereJwtTokenElements)
        val token2 = jwtTokenIssuer.createAccessToken(imHereJwtTokenElements)

        // then
        val secretKey = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))

        val claims1 = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token1)
            .payload

        val claims2 = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token2)
            .payload

        assertThat(claims1.id).isNotEqualTo(claims2.id)
    }
}

