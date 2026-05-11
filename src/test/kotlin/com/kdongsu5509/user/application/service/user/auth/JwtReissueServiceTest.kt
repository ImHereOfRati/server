package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenProviderPort
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class JwtReissueServiceTest {

    @Mock
    private lateinit var tokenProvider: ImHereTokenProviderPort

    @InjectMocks
    private lateinit var jwtReissueService: JwtReissueService

    @Test
    @DisplayName("RefreshToken을 사용하여 JWT를 재발급한다")
    fun reissueByRefreshToken_success() {
        // given
        val refreshToken = "test-refresh-token"
        val expectedJwt = ImHereJwt("access", "refresh")
        given(tokenProvider.reissueByRefreshToken(refreshToken)).willReturn(expectedJwt)

        // when
        jwtReissueService.reissueByRefreshToken(refreshToken)

        // then
        then(tokenProvider).should().reissueByRefreshToken(refreshToken)
    }

    @Test
    @DisplayName("이메일을 사용하여 JWT를 재발급한다")
    fun reissueByUserEmail_success() {
        // given
        val email = "test@example.com"
        val expectedJwt = ImHereJwt("access", "refresh")
        given(tokenProvider.reissueByEmail(email)).willReturn(expectedJwt)

        // when
        jwtReissueService.reissueByUserEmail(email)

        // then
        then(tokenProvider).should().reissueByEmail(email)
    }
}
