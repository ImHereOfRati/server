package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.port.out.user.CachePort
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SelfSignedTokenProviderTest {

    companion object {
        const val USERNAME = "test@example.com"

        const val ROLE = "USER"
        const val TOKEN_INFO_ROLE = "ROLE_USER"

        const val ACCESS_TOKEN = "access-token"
        const val REFRESH_TOKEN = "refresh-token"

        const val NEW_ACCESS_TOKEN = "new-access-token"
        const val NEW_REFRESH_TOKEN = "new-refresh-token"

        var EXP_DATE = LocalDateTime.now().plusDays(7)!!
        const val REDIS_KEY = "refresh:$USERNAME"
    }

    @Mock
    private lateinit var jwtTokenIssuer: JwtTokenIssuer

    @Mock
    private lateinit var jwtTokenUtil: JwtTokenUtil

    @Mock
    private lateinit var cachePort: CachePort

    private lateinit var selfSignedTokenProvider: SelfSignedTokenProvider

    @BeforeEach
    fun setUp() {
        selfSignedTokenProvider = SelfSignedTokenProvider(jwtTokenIssuer, jwtTokenUtil, cachePort)
    }

    @Test
    @DisplayName("JWT 인증 토큰을 성공적으로 발급한다")
    fun issueJwtAuth_success() {
        // given
        `when`(jwtTokenIssuer.createAccessToken(USERNAME, ROLE)).thenReturn(ACCESS_TOKEN)
        `when`(jwtTokenIssuer.createRefreshToken(USERNAME, ROLE)).thenReturn(REFRESH_TOKEN)
        `when`(jwtTokenUtil.getExpirationDateFromToken(REFRESH_TOKEN)).thenReturn(EXP_DATE)

        // when
        val result = selfSignedTokenProvider.issueJwtToken(USERNAME, ROLE)

        // then
        assertThat(result).isNotNull()
        assertThat(result.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(REFRESH_TOKEN)

        verify(jwtTokenIssuer).createAccessToken(USERNAME, ROLE)
        verify(jwtTokenIssuer).createRefreshToken(USERNAME, ROLE)
        verify(jwtTokenUtil).getExpirationDateFromToken(REFRESH_TOKEN)
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 JWT 토큰을 재발급한다")
    fun reissueJwtToken_validRefreshToken_success() {
        // given
        `when`(jwtTokenUtil.getUsernameFromToken(REFRESH_TOKEN)).thenReturn(USERNAME)
        `when`(jwtTokenUtil.getRoleFromToken(REFRESH_TOKEN)).thenReturn(TOKEN_INFO_ROLE)
        `when`(jwtTokenUtil.validateToken(REFRESH_TOKEN)).thenReturn(true)
        `when`(cachePort.find(REDIS_KEY)).thenReturn(REFRESH_TOKEN)
        `when`(jwtTokenIssuer.createAccessToken(USERNAME, TOKEN_INFO_ROLE)).thenReturn(NEW_ACCESS_TOKEN)
        `when`(jwtTokenIssuer.createRefreshToken(USERNAME, TOKEN_INFO_ROLE)).thenReturn(NEW_REFRESH_TOKEN)
        `when`(jwtTokenUtil.getExpirationDateFromToken(NEW_REFRESH_TOKEN)).thenReturn(EXP_DATE)

        // when
        val result = selfSignedTokenProvider.reissueJwtToken(REFRESH_TOKEN)

        // then
        assertThat(result).isNotNull()
        assertThat(result.accessToken).isEqualTo(NEW_ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(NEW_REFRESH_TOKEN)

        verify(jwtTokenUtil).getUsernameFromToken(REFRESH_TOKEN)
        verify(jwtTokenUtil).getRoleFromToken(REFRESH_TOKEN)
        verify(jwtTokenUtil).validateToken(REFRESH_TOKEN)
        verify(cachePort).find(REDIS_KEY)
        verify(jwtTokenIssuer).createAccessToken(USERNAME, TOKEN_INFO_ROLE)
        verify(jwtTokenIssuer).createRefreshToken(USERNAME, TOKEN_INFO_ROLE)
        verify(jwtTokenUtil).getExpirationDateFromToken(NEW_REFRESH_TOKEN)

        val keyCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)
        val durationCaptor: ArgumentCaptor<Duration> = ArgumentCaptor.forClass(Duration::class.java)
        verify(cachePort).save(capture(keyCaptor), capture(valueCaptor), capture(durationCaptor))
        assertThat(keyCaptor.value).isEqualTo("refresh:$USERNAME")
        assertThat(valueCaptor.value).isEqualTo(NEW_REFRESH_TOKEN)
        assertThat(durationCaptor.value).isNotNull()
    }

    fun <T> capture(captor: ArgumentCaptor<T>): T = captor.capture()


    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시 예외가 발생한다")
    fun reissueJwtToken_invalidRefreshToken_throwsException() {
        // given
        val invalidRefreshToken = "invalid-refresh-token"

        `when`(jwtTokenUtil.getUsernameFromToken(invalidRefreshToken)).thenReturn(USERNAME)
        `when`(jwtTokenUtil.getRoleFromToken(invalidRefreshToken)).thenReturn(TOKEN_INFO_ROLE)
        `when`(jwtTokenUtil.validateToken(invalidRefreshToken)).thenReturn(false)

        // when & then
        assertThrows<BusinessException> {
            selfSignedTokenProvider.reissueJwtToken(invalidRefreshToken)
        }.also { exception ->
            assertThat(exception.message).isEqualTo("잘못된 토큰입니다")
        }

        verify(jwtTokenUtil).validateToken(invalidRefreshToken)
    }

    @Test
    @DisplayName("만료된 리프레시 토큰으로 재발급 시 예외가 발생한다")
    fun reissueJwtToken_expiredRefreshToken_throwsException() {
        // given
        val expiredRefreshToken = "expired-refresh-token"

        `when`(jwtTokenUtil.getUsernameFromToken(expiredRefreshToken)).thenReturn(USERNAME)
        `when`(jwtTokenUtil.getRoleFromToken(expiredRefreshToken)).thenReturn(TOKEN_INFO_ROLE)
        `when`(jwtTokenUtil.validateToken(expiredRefreshToken)).thenReturn(false)

        // when & then
        assertThrows<BusinessException> {
            selfSignedTokenProvider.reissueJwtToken(expiredRefreshToken)
        }.also { exception ->
            assertThat(exception.message).isEqualTo("잘못된 토큰입니다")
        }

        verify(jwtTokenUtil).validateToken(expiredRefreshToken)
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 일치하지 않는 리프레시 토큰으로 재발급 시 예외가 발생한다")
    fun reissueJwtToken_mismatchedRefreshToken_throwsException() {
        // given
        val refreshToken = "refresh-token"
        val differentTokenInRedis = "different-refresh-token"
        val username = "test@example.com"
        val role = "ROLE_USER"
        val redisKey = "refresh:$username"

        `when`(jwtTokenUtil.getUsernameFromToken(refreshToken)).thenReturn(username)
        `when`(jwtTokenUtil.getRoleFromToken(refreshToken)).thenReturn(role)
        `when`(jwtTokenUtil.validateToken(refreshToken)).thenReturn(true)
        `when`(cachePort.find(redisKey)).thenReturn(differentTokenInRedis)

        // when & then
        assertThrows<IllegalArgumentException> {
            selfSignedTokenProvider.reissueJwtToken(refreshToken)
        }.also { exception ->
            assertThat(exception.message).isEqualTo("일치하지 않는 리프레시 토큰")
        }

        verify(cachePort).find(redisKey)
    }

    @Test
    @DisplayName("Redis에 리프레시 토큰이 없을 때 재발급 시 예외가 발생한다")
    fun reissueJwtToken_noTokenInRedis_throwsException() {
        // given
        `when`(jwtTokenUtil.getUsernameFromToken(REFRESH_TOKEN)).thenReturn(USERNAME)
        `when`(jwtTokenUtil.getRoleFromToken(REFRESH_TOKEN)).thenReturn(TOKEN_INFO_ROLE)
        `when`(jwtTokenUtil.validateToken(REFRESH_TOKEN)).thenReturn(true)
        `when`(cachePort.find(REDIS_KEY)).thenReturn(null)

        // when & then
        assertThrows<IllegalArgumentException> {
            selfSignedTokenProvider.reissueJwtToken(REFRESH_TOKEN)
        }.also { exception ->
            assertThat(exception.message).isEqualTo("일치하지 않는 리프레시 토큰")
        }

        verify(cachePort).find(REDIS_KEY)
    }

    @Test
    @DisplayName("재발급 시 토큰에서 추출한 role을 그대로 전달한다")
    fun reissueJwtToken_passesRoleAsIs() {
        // given
        val expirationDate = LocalDateTime.now().plusDays(1)

        `when`(jwtTokenUtil.getUsernameFromToken(REFRESH_TOKEN)).thenReturn(USERNAME)
        `when`(jwtTokenUtil.getRoleFromToken(REFRESH_TOKEN)).thenReturn(TOKEN_INFO_ROLE)
        `when`(jwtTokenUtil.validateToken(REFRESH_TOKEN)).thenReturn(true)
        `when`(cachePort.find(REDIS_KEY)).thenReturn(REFRESH_TOKEN)
        `when`(jwtTokenIssuer.createAccessToken(USERNAME, TOKEN_INFO_ROLE)).thenReturn(NEW_ACCESS_TOKEN)
        `when`(jwtTokenIssuer.createRefreshToken(USERNAME, TOKEN_INFO_ROLE)).thenReturn(NEW_REFRESH_TOKEN)
        `when`(jwtTokenUtil.getExpirationDateFromToken(NEW_REFRESH_TOKEN)).thenReturn(expirationDate)

        // when
        val result = selfSignedTokenProvider.reissueJwtToken(REFRESH_TOKEN)

        // then
        assertThat(result).isNotNull()
        assertThat(result.accessToken).isEqualTo(NEW_ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(NEW_REFRESH_TOKEN)
        verify(jwtTokenIssuer).createAccessToken(USERNAME, TOKEN_INFO_ROLE)
        verify(jwtTokenIssuer).createRefreshToken(USERNAME, TOKEN_INFO_ROLE)
        verify(jwtTokenUtil).getExpirationDateFromToken(NEW_REFRESH_TOKEN)
    }
}

