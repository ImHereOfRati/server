package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.application.port.out.UserLoadPort
import com.kdongsu5509.user.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import java.util.*

@ExtendWith(MockitoExtension::class)
class LoginServiceTest {

    companion object {
        const val TEST_ID_TOKEN = "idToken"
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "홍길동"
        const val TEST_ACCESS_TOKEN = "accessToken"
        const val TEST_REFRESH_TOKEN = "refreshToken"

        val TEST_OAUTH_PROVIDER = OAuth2Provider.KAKAO
        val TEST_OIDC_USER_INFO = OIDCUserInfo(email = TEST_EMAIL, nickname = TEST_NICKNAME)
        val TEST_USER = User(
            id = UUID.randomUUID(),
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = TEST_OAUTH_PROVIDER,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE,
        )
    }

    @Mock
    lateinit var oidcVerifyPort: OIDCVerifyPort

    @Mock
    lateinit var userLoadPort: UserLoadPort

    @Mock
    lateinit var tokenProviderPort: ImHereTokenProviderPort

    @InjectMocks
    lateinit var loginService: LoginService

    @Test
    @DisplayName("등록된 사용자가 OIDC 토큰으로 로그인을 시도하면 JWT 토큰을 발급한다")
    fun login_success() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willReturn(TEST_OIDC_USER_INFO)
        given(userLoadPort.findByEmail(TEST_EMAIL)).willReturn(TEST_USER)
        given(tokenProviderPort.issue(any())).willReturn(ImHereJwtToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN))

        // when
        val result = loginService.login(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)

        // then
        assertThat(result.accessToken).isEqualTo(TEST_ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(TEST_REFRESH_TOKEN)

        then(oidcVerifyPort).should().verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        then(userLoadPort).should().findByEmail(TEST_EMAIL)
        then(tokenProviderPort).should().issue(any())
    }

    @Test
    @DisplayName("사용자 조회 시 등록되지 않은 사용자이면 예외가 발생하고 전파가 된다")
    fun login_fail_user_not_registered() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willReturn(TEST_OIDC_USER_INFO)
        given(userLoadPort.findByEmail(TEST_EMAIL)).willReturn(null)

        // when & then
        val exception = assertThrows<ImHereBaseException> {
            loginService.login(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        }

        assertThat(exception.errorCode).isEqualTo(AuthException.USER_NOT_REGISTER)
        then(oidcVerifyPort).should().verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        then(userLoadPort).should().findByEmail(TEST_EMAIL)
        then(tokenProviderPort).shouldHaveNoInteractions()
    }
}
