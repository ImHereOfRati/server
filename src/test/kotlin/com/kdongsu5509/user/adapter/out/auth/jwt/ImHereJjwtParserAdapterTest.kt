package com.kdongsu5509.user.adapter.out.auth.jwt

import com.common.testUtil.TestJwtBuilder
import com.kdongsu5509.support.exception.type.UnauthorizedException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@ExtendWith(MockitoExtension::class)
class ImHereJjwtParserAdapterTest {

    @Mock
    private lateinit var keyProvider: ImHereJjwtKeyProvider

    private lateinit var imHereJjwtParserAdapter: ImHereJjwtParserAdapter

    private val secretKey: SecretKey =
        Keys.hmacShaKeyFor(TestJwtBuilder.TEST_IMHERE_JWT_SECRET.toByteArray(StandardCharsets.UTF_8))

    @BeforeEach
    fun setUp() {
        `when`(keyProvider.secretKey).thenReturn(secretKey)
        imHereJjwtParserAdapter = ImHereJjwtParserAdapter(keyProvider)
    }

    @Test
    @DisplayName("유효한 ImHere Access Token을 성공적으로 파싱하여 클레임을 반환한다")
    fun parse_success() {
        // given
        val email = "test@example.com"
        val nickname = "testUser"
        val role = "NORMAL"
        val status = "ACTIVE"
        val uid = UUID.randomUUID()
        val token = TestJwtBuilder.buildImHereAccessToken(email, nickname, role, status, uid)

        // when
        val result = imHereJjwtParserAdapter.parse(token)

        // then
        assertThat(result.email).isEqualTo(email)
        assertThat(result.nickname).isEqualTo(nickname)
        assertThat(result.role).isEqualTo(role)
        assertThat(result.status).isEqualTo(status)
        assertThat(result.uid).isEqualTo(uid)
        assertThat(result.expiration).isNotNull()
    }

    @Test
    @DisplayName("유효한 토큰인 경우 validate 결과가 true를 반환한다")
    fun validate_success() {
        // given
        val token = TestJwtBuilder.buildImHereAccessToken("test@test.com", "nick")

        // when
        val result = imHereJjwtParserAdapter.validate(token)

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("만료된 토큰의 경우 validate 시 UnauthorizedException(TOKEN_001)을 발생시킨다")
    fun validate_expiredToken_throwsException() {
        // given
        val now = Instant.now()
        val expiredToken = Jwts.builder()
            .claim(JwtClaimKeys.CLAIM_USER_ID, UUID.randomUUID().toString())
            .claim(JwtClaimKeys.CLAIM_EMAIL, "expired@test.com")
            .claim(JwtClaimKeys.CLAIM_NICKNAME, "expired")
            .claim(JwtClaimKeys.CLAIM_ROLE, "ROLE_NORMAL")
            .claim(JwtClaimKeys.CLAIM_STATUS, "ACTIVE")
            .issuedAt(Date.from(now.minusSeconds(3600)))
            .expiration(Date.from(now.minusSeconds(1800))) // 30분 전 만료
            .signWith(secretKey)
            .compact()

        // when & then
        assertThrows<UnauthorizedException> {
            imHereJjwtParserAdapter.validate(expiredToken)
        }.also {
            assertThat(it.message).contains("만료된 토큰입니다.")
        }
    }

    @Test
    @DisplayName("서명이 일치하지 않는 유효하지 않은 토큰인 경우 UnauthorizedException(TOKEN_002)을 발생시킨다")
    fun validate_invalidSignature_throwsException() {
        // given
        val otherSecretKey =
            Keys.hmacShaKeyFor("differentSecretKeyForTestingPurpose123456".toByteArray(StandardCharsets.UTF_8))
        val invalidToken = Jwts.builder()
            .subject("test")
            .signWith(otherSecretKey)
            .compact()

        // when & then
        assertThrows<UnauthorizedException> {
            imHereJjwtParserAdapter.validate(invalidToken)
        }.also {
            assertThat(it.message).contains("유효하지 않은 토큰입니다.")
        }
    }

    @Test
    @DisplayName("형식이 잘못된 토큰인 경우 UnauthorizedException(TOKEN_002)을 발생시킨다")
    fun validate_malformedToken_throwsException() {
        // given
        val malformedToken = "not.a.jwt.token"

        // when & then
        assertThrows<UnauthorizedException> {
            imHereJjwtParserAdapter.validate(malformedToken)
        }.also {
            assertThat(it.message).contains("유효하지 않은 토큰입니다.")
        }
    }
}
