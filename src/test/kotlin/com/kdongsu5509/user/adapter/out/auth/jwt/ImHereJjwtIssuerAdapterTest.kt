package com.kdongsu5509.user.adapter.out.auth.jwt

import com.common.testUtil.TestJwtBuilder
import com.kdongsu5509.user.application.dto.JwtTokenClaims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.charset.StandardCharsets
import java.util.*
import javax.crypto.SecretKey

@ExtendWith(MockitoExtension::class)
class ImHereJjwtIssuerAdapterTest {

    @Mock
    private lateinit var imHereJwtProperties: ImHereJwtProperties

    @Mock
    private lateinit var keyProvider: ImHereJjwtKeyProvider

    private lateinit var imHereJjwtIssuerAdapter: ImHereJjwtIssuerAdapter

    private val secretKey: SecretKey =
        Keys.hmacShaKeyFor(TestJwtBuilder.TEST_IMHERE_JWT_SECRET.toByteArray(StandardCharsets.UTF_8))

    private val claims = JwtTokenClaims(
        uid = UUID.randomUUID(),
        email = "test@example.com",
        nickname = "tester",
        role = "NORMAL",
        status = "ACTIVE"
    )

    @BeforeEach
    fun setUp() {
        `when`(keyProvider.secretKey).thenReturn(secretKey)
        imHereJjwtIssuerAdapter = ImHereJjwtIssuerAdapter(imHereJwtProperties, keyProvider)
    }

    @Test
    @DisplayName("Access Token을 요청된 규격에 맞게 생성한다")
    fun createAccessToken_success() {
        // given
        `when`(imHereJwtProperties.accessExpirationMinutes).thenReturn(30L)

        // when
        val token = imHereJjwtIssuerAdapter.createAccessToken(claims)

        // then
        val parsedClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(parsedClaims[JwtClaimKeys.CLAIM_CATEGORY]).isEqualTo(JwtClaimKeys.ACCESS_TOKEN)
        assertThat(parsedClaims[JwtClaimKeys.CLAIM_USER_ID]).isEqualTo(claims.uid.toString())
        assertThat(parsedClaims[JwtClaimKeys.CLAIM_EMAIL]).isEqualTo(claims.email)
        assertThat(parsedClaims[JwtClaimKeys.CLAIM_ROLE]).isEqualTo("ROLE_${claims.role}")
        assertThat(parsedClaims.expiration).isAfter(Date())
    }

    @Test
    @DisplayName("Refresh Token을 요청된 규격에 맞게 생성한다")
    fun createRefreshToken_success() {
        // given
        `when`(imHereJwtProperties.refreshExpirationDays).thenReturn(7L)

        // when
        val token = imHereJjwtIssuerAdapter.createRefreshToken(claims)

        // then
        val parsedClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(parsedClaims[JwtClaimKeys.CLAIM_CATEGORY]).isEqualTo(JwtClaimKeys.REFRESH_TOKEN)
        assertThat(parsedClaims[JwtClaimKeys.CLAIM_USER_ID]).isEqualTo(claims.uid.toString())
        assertThat(parsedClaims.expiration).isAfter(Date())
    }

    @Test
    @DisplayName("Admin Access Token을 요청된 규격에 맞게 생성한다")
    fun createAdminAccessToken_success() {
        // given
        `when`(imHereJwtProperties.adminExpirationMinutes).thenReturn(60L)

        // when
        val token = imHereJjwtIssuerAdapter.createAdminAccessToken(claims)

        // then
        val parsedClaims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload

        assertThat(parsedClaims[JwtClaimKeys.CLAIM_CATEGORY]).isEqualTo(JwtClaimKeys.ACCESS_TOKEN)
        assertThat(parsedClaims[JwtClaimKeys.CLAIM_ROLE]).isEqualTo("ROLE_${claims.role}")
        assertThat(parsedClaims.expiration).isAfter(Date())
    }
}
