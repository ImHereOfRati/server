package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
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
        val expectedJwt = ImHereJwtToken("access", "refresh")
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
        val expectedJwt = ImHereJwtToken("access", "refresh")
        given(tokenProvider.reissueByEmail(email)).willReturn(expectedJwt)

        // when
        jwtReissueService.reissueByUserEmail(email)

        // then
        then(tokenProvider).should().reissueByEmail(email)
    }
}
