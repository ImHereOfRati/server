package com.kdongsu5509.auth.adapter.out.jwt


import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.CachePort
import com.kdongsu5509.auth.application.port.out.ImHereTokenIssuerPort
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.support.exception.type.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class ImHereTokenProviderAdapterTest {

    companion object {
        const val USER_EMAIL = "test@example.com"
        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"
        const val REFRESH_EXP_DAYS = 7L
        const val REDIS_KEY = "refresh:$USER_EMAIL"

        val TEST_UUID: UUID = UUID.randomUUID()
    }

    @Mock
    private lateinit var tokenIssuer: ImHereTokenIssuerPort

    @Mock
    private lateinit var tokenParser: ImHereTokenParserPort

    @Mock
    private lateinit var cachePort: CachePort

    private lateinit var tokenProvider: ImHereTokenProviderAdapter
    private lateinit var jwtTokenClaims: JwtTokenClaims

    @BeforeEach
    fun setUp() {
        val properties = ImHereJwtProperties(
            secret = "test-secret-key-at-least-32-characters-long",
            accessExpirationMinutes = 60,
            refreshExpirationDays = REFRESH_EXP_DAYS
        )

        tokenProvider = ImHereTokenProviderAdapter(tokenIssuer, tokenParser, cachePort, properties)

        jwtTokenClaims = JwtTokenClaims(
            uid = TEST_UUID,
            email = USER_EMAIL,
            nickname = "테스트",
            role = "USER",
            status = "ACTIVE",
            expiration = LocalDateTime.now().plusDays(7)
        )
    }

    @Test
    @DisplayName("JWT 인증 토큰을 성공적으로 발급한다")
    fun issue_success() {
        // given
        given(tokenIssuer.createAccessToken(jwtTokenClaims)).willReturn(ACCESS_TOKEN)
        given(tokenIssuer.createRefreshToken(jwtTokenClaims)).willReturn(REFRESH_TOKEN)

        // when
        val result = tokenProvider.issue(jwtTokenClaims)

        // then
        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)

        then(cachePort).should().save(REDIS_KEY, REFRESH_TOKEN, Duration.ofDays(REFRESH_EXP_DAYS))
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 JWT 토큰을 재발급한다")
    fun reissue_validRefreshToken_success() {
        // given
        given(tokenParser.parse(REFRESH_TOKEN)).willReturn(jwtTokenClaims)
        given(cachePort.find(REDIS_KEY, String::class.java)).willReturn(REFRESH_TOKEN)
        given(tokenIssuer.createAccessToken(jwtTokenClaims)).willReturn(ACCESS_TOKEN)
        given(tokenIssuer.createRefreshToken(jwtTokenClaims)).willReturn(REFRESH_TOKEN)

        // when
        val result = tokenProvider.reissueByRefreshToken(REFRESH_TOKEN)

        // then
        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)

        then(tokenParser).should().validate(REFRESH_TOKEN)
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시 예외가 발생한다")
    fun reissue_invalidRefreshToken_throwsException() {
        // given
        val invalidToken = "invalid-token"
        given(tokenParser.validate(invalidToken)).willThrow(UnauthorizedException(AuthException.IMHERE_INVALID_TOKEN.errorMessage))

        // when & then
        assertAuthError(AuthException.IMHERE_INVALID_TOKEN) {
            tokenProvider.reissueByRefreshToken(invalidToken)
        }
    }

    @Test
    @DisplayName("Cache에 저장된 토큰이 없는 경우 재발급 시 예외가 발생한다")
    fun reissue_refreshTokenNotFound_throwsException() {
        // given
        given(cachePort.find(REDIS_KEY, String::class.java))
            .willReturn(null)

        // when & then
        assertThrows<UnauthorizedException> {
            tokenProvider.reissueByEmail(USER_EMAIL)
        }.also {
        assertThat(it.message).isEqualTo(AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.errorMessage)
        }
    }

    @Test
    @DisplayName("Cache에 저장된 토큰과 일치하지 않는 리프시 토큰으로 재발급 시 예외가 발생한다")
    fun reissue_mismatchedRefreshToken_throwsException() {
        // given
        given(tokenParser.parse(REFRESH_TOKEN)).willReturn(jwtTokenClaims)
        given(cachePort.find(REDIS_KEY, String::class.java)).willReturn("different-token")

        // when & then
        assertAuthError(AuthException.IMHERE_INVALID_TOKEN) {
            tokenProvider.reissueByRefreshToken(REFRESH_TOKEN)
        }
    }

    @Test
    @DisplayName("이메일 기반으로 성공적으로 토큰을 재발급한다")
    fun reissue_by_email_success() {
        // given
        given(cachePort.find(REDIS_KEY, String::class.java)).willReturn(REFRESH_TOKEN)
        given(tokenParser.parse(REFRESH_TOKEN)).willReturn(jwtTokenClaims)
        given(tokenIssuer.createAccessToken(jwtTokenClaims)).willReturn(ACCESS_TOKEN)
        given(tokenIssuer.createRefreshToken(jwtTokenClaims)).willReturn(REFRESH_TOKEN)

        // when
        val result = tokenProvider.reissueByEmail(USER_EMAIL)

        // then
        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)
    }

    @Test
    @DisplayName("이메일로 재발급 시 Cache에 토큰이 없으면 예외가 발생한다")
    fun reissue_by_email_notFound_throwsException() {
        // given
        given(
            cachePort.find(REDIS_KEY, String::class.java)
        ).willReturn(null)

        // when & then
        assertThrows<UnauthorizedException> {
            tokenProvider.reissueByEmail(USER_EMAIL)
        }.also {
        assertThat(it.message).isEqualTo(AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.errorMessage)
        }
    }

    private fun assertAuthError(expectedError: AuthException, block: () -> Unit) {
        val exception = assertThrows<UnauthorizedException>(block)
        assertThat(exception.message).isEqualTo(expectedError.errorMessage)
    }
}
