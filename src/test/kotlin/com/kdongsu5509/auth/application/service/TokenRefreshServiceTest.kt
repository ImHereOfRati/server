package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class TokenRefreshServiceTest {

    @Mock
    private lateinit var tokenProvider: ImHereTokenProviderPort

    @InjectMocks
    private lateinit var tokenRefreshService: TokenRefreshService

    @Test
    @DisplayName("RefreshToken을 사용하여 JWT를 갱신한다")
    fun refresh_success() {
        // given
        val refreshToken = "valid-refresh-token"
        val expectedJwt = ImHereJwtToken("access", "refresh")
        given(tokenProvider.reissueByRefreshToken(refreshToken)).willReturn(expectedJwt)

        // when
        val result = tokenRefreshService.refresh(refreshToken)

        // then
        assertThat(result).isEqualTo(expectedJwt)
    }

    @Test
    @DisplayName("RefreshToken 갱신 중 예외가 발생하면 그대로 전파된다")
    fun refresh_exception_propagation() {
        // given
        val refreshToken = "invalid-refresh-token"
        given(tokenProvider.reissueByRefreshToken(refreshToken))
            .willThrow(ImHereBaseException(AuthException.IMHERE_INVALID_TOKEN))

        // when & then
        assertThatThrownBy { tokenRefreshService.refresh(refreshToken) }
            .isInstanceOf(ImHereBaseException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthException.IMHERE_INVALID_TOKEN)
    }
}
