package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.AuthenticationProcessResult
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.application.port.out.user.oauth.OIDCVerifyPort
import com.kdongsu5509.user.domain.user.OAuth2Provider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OIDCAuthenticationServiceTest {

    companion object {
        private const val ID_TOKEN = "test-id-token"
        private const val EMAIL = "test@kakao.com"
        private const val NICKNAME = "홍길동"
        private val PROVIDER = OAuth2Provider.KAKAO

        private val USER_INFO = OIDCUserInfo(email = EMAIL, nickname = NICKNAME)
    }

    @Mock
    private lateinit var oidcVerifyPort: OIDCVerifyPort

    @Mock
    private lateinit var userLoginService: UserLoginService

    private lateinit var authService: OIDCAuthenticationService

    @BeforeEach
    fun setUp() {
        authService = OIDCAuthenticationService(oidcVerifyPort, userLoginService)
    }

    @Test
    @DisplayName("OIDC 토큰으로 인증을 시도하여 성공하면 로그인 결과를 반환한다")
    fun authenticate_success() {
        // given
        val expectedResult = AuthenticationProcessResult(false, "access", "refresh")
        given(oidcVerifyPort.verify(ID_TOKEN)).willReturn(USER_INFO)
        given(userLoginService.loginOrRegister(EMAIL, NICKNAME, PROVIDER)).willReturn(expectedResult)

        // when
        val result = authService.authenticate(ID_TOKEN, PROVIDER)

        // then
        assertThat(result).isEqualTo(expectedResult)

        then(oidcVerifyPort).should().verify(ID_TOKEN)
        then(userLoginService).should().loginOrRegister(EMAIL, NICKNAME, PROVIDER)
    }

    @Test
    @DisplayName("OIDC 토큰 검증만 수행하여 유저 정보를 반환한다")
    fun verifyOIDC_success() {
        // given
        given(oidcVerifyPort.verify(ID_TOKEN)).willReturn(USER_INFO)

        // when
        val result = authService.verifyOIDC(ID_TOKEN, PROVIDER)

        // then
        assertThat(result).isEqualTo(USER_INFO)
        then(oidcVerifyPort).should().verify(ID_TOKEN)
    }
}
