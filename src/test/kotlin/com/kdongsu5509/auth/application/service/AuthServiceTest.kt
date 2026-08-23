package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.api.RegisterUserCommand
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserRegistrationContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
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
class AuthServiceTest {

    companion object {
        const val TEST_ID_TOKEN = "idToken"
        const val TEST_NONCE = "nonce"
        const val TEST_ACCESS_TOKEN = "accessToken"
        const val TEST_REFRESH_TOKEN = "refreshToken"
        const val TEST_EMAIL = "ds.ko@kakao.com"
        const val TEST_NICKNAME = "고동수"
        const val TEST_SUB = "sub-123"

        val TEST_OAUTH_PROVIDER = OAuth2Provider.KAKAO
        val TEST_OIDC_USER_INFO = OIDCUserInfo(email = TEST_EMAIL, nickname = TEST_NICKNAME, sub = TEST_SUB)
        val TEST_REGISTER_COMMAND = RegisterUserCommand(
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = TEST_OAUTH_PROVIDER,
            oidcSubject = TEST_SUB
        )

        fun userResult(status: UserStatus) = UserResult(
            id = UUID.randomUUID(),
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = TEST_OAUTH_PROVIDER,
            role = UserRole.NORMAL,
            status = status,
            oidcSubject = TEST_SUB
        )
    }

    @Mock
    lateinit var oidcVerifyPort: OIDCVerifyPort

    @Mock
    lateinit var tokenProviderPort: ImHereTokenProviderPort

    @Mock
    lateinit var userLookupContract: UserLookupContract

    @Mock
    lateinit var userRegistrationContract: UserRegistrationContract

    @InjectMocks
    lateinit var authService: AuthService

    @Test
    @DisplayName("계정이 없으면 새로 만들고 JWT 토큰을 발급한다")
    fun auth_registers_new_user() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(null)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND)).willReturn(userResult(UserStatus.PENDING))
        givenIssuedToken()

        // when
        val result = authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)

        // then
        assertThat(result.accessToken).isEqualTo(TEST_ACCESS_TOKEN)
        assertThat(result.refreshToken).isEqualTo(TEST_REFRESH_TOKEN)

        then(oidcVerifyPort).should().verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        then(userRegistrationContract).should().register(TEST_REGISTER_COMMAND)
        then(tokenProviderPort).should().issue(any())
    }

    @Test
    @DisplayName("이미 가입된 계정이면 새로 만들지 않고 그대로 JWT 토큰을 발급한다")
    fun auth_logs_in_existing_user() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(userResult(UserStatus.ACTIVE))
        givenIssuedToken()

        // when
        val result = authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)

        // then
        assertThat(result.accessToken).isEqualTo(TEST_ACCESS_TOKEN)
        then(userRegistrationContract).shouldHaveNoInteractions()
        then(tokenProviderPort).should().issue(any())
    }

    @Test
    @DisplayName("OIDC subject로 조회되지 않으면 이메일로 기존 사용자를 찾지 않고 신규 가입 흐름을 사용한다")
    fun auth_does_not_fallback_to_email() {
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(null)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND)).willReturn(userResult(UserStatus.PENDING))
        givenIssuedToken()

        authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)

        then(userLookupContract).should().findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)
        then(userLookupContract).shouldHaveNoMoreInteractions()
    }

    @Test
    @DisplayName("기존 계정의 OAuth 제공자나 subject가 다르면 로그인시키지 않는다")
    fun auth_rejects_mismatched_oidc_identity() {
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(
            userResult(UserStatus.ACTIVE).copy(
                oauthProvider = OAuth2Provider.GOOGLE,
                oidcSubject = "different-sub"
            )
        )

        val exception = assertThrows<ImHereBaseException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.errorCode).isEqualTo(AuthException.OIDC_FORMAT_INVALID)
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("정지된 계정이면 토큰을 발급하지 않는다")
    fun auth_fail_blocked_user() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(userResult(UserStatus.BLOCKED))

        // when & then
        val exception = assertThrows<ImHereBaseException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.errorCode).isEqualTo(AuthException.USER_DISABLED)
        then(userRegistrationContract).shouldHaveNoInteractions()
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("탈퇴한 계정과 같은 이메일이면 재가입도 로그인도 할 수 없다")
    fun auth_fail_withdrawn_user() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(userResult(UserStatus.WITHDRAWN))

        // when & then
        val exception = assertThrows<ImHereBaseException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.errorCode).isEqualTo(AuthException.USER_WITHDRAWN)
        then(userRegistrationContract).shouldHaveNoInteractions()
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("OIDC 검증 시 예외가 발생하면 전파가 된다")
    fun auth_fail_oidc_verification() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE))
            .willThrow(RuntimeException("OIDC Verification Failed"))

        // when & then
        val exception = assertThrows<RuntimeException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.message).isEqualTo("OIDC Verification Failed")
        then(userLookupContract).shouldHaveNoInteractions()
        then(userRegistrationContract).shouldHaveNoInteractions()
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("사용자 저장 시 예외가 발생하면 전파가 된다")
    fun auth_fail_user_save() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(null)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND))
            .willThrow(RuntimeException("Persistence Failed"))

        // when & then
        val exception = assertThrows<RuntimeException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.message).isEqualTo("Persistence Failed")
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("토큰 발급 시 예외가 발생하면 전파가 된다")
    fun auth_fail_token_issue() {
        // given
        givenVerifiedOidcUser()
        given(userLookupContract.findByOidcIdentityOrNull(TEST_OAUTH_PROVIDER, TEST_SUB)).willReturn(userResult(UserStatus.ACTIVE))
        given(tokenProviderPort.issue(any())).willThrow(RuntimeException("Token Issue Failed"))

        // when & then
        val exception = assertThrows<RuntimeException> {
            authService.auth(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)
        }

        assertThat(exception.message).isEqualTo("Token Issue Failed")
    }

    private fun givenVerifiedOidcUser() {
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN, TEST_NONCE)).willReturn(TEST_OIDC_USER_INFO)
    }

    private fun givenIssuedToken() {
        given(tokenProviderPort.issue(any())).willReturn(ImHereJwtToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN))
    }
}
