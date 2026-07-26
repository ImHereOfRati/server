package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import java.util.*

@ExtendWith(MockitoExtension::class)
class RegisterServiceTest {

    companion object {
        const val TEST_ID_TOKEN = "idToken"
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "홍길동"
        const val TEST_SUB = "sub-123"
        const val TEST_ACCESS_TOKEN = "token"
        const val TEST_REFRESH_TOKEN = "refresh"

        val TEST_OAUTH_PROVIDER = OAuth2Provider.KAKAO
        val TEST_OIDC_USER_INFO = OIDCUserInfo(email = TEST_EMAIL, nickname = TEST_NICKNAME, sub = TEST_SUB)
        val TEST_REGISTER_COMMAND = RegisterUserCommand(
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = TEST_OAUTH_PROVIDER,
            oidcSubject = TEST_SUB
        )
        val TEST_USER_RESULT = UserResult(
            id = UUID.randomUUID(),
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = TEST_OAUTH_PROVIDER,
            role = UserRole.NORMAL,
            status = UserStatus.PENDING,
            oidcSubject = TEST_SUB
        )

    }

    @Mock
    lateinit var oidcVerifyPort: OIDCVerifyPort

    @Mock
    lateinit var userLookupContract: UserLookupContract

    @Mock
    lateinit var userRegistrationContract: UserRegistrationContract

    @Mock
    lateinit var tokenProviderPort: ImHereTokenProviderPort

    @InjectMocks
    lateinit var registerService: RegisterService

    @Test
    @DisplayName("외부 요소들과 협력하여 사용자 등록을 잘 한다")
    fun register_success() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willReturn(TEST_OIDC_USER_INFO)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND)).willReturn(TEST_USER_RESULT)
        given(tokenProviderPort.issue(any())).willReturn(ImHereJwtToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN))

        // when
        registerService.register(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)

        // then
        then(oidcVerifyPort).should().verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        then(userLookupContract).should().findByEmailOrNull(TEST_EMAIL)
        then(userRegistrationContract).should().register(TEST_REGISTER_COMMAND)
        then(tokenProviderPort).should().issue(any())
    }

    @Test
    @DisplayName("OIDC 검증 시 예외가 발생하면 전파가 된다")
    fun register_fail_oidc_verification() {
        // given
        val expectedException = RuntimeException("OIDC Verification Failed")
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willThrow(expectedException)

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<RuntimeException> {
            registerService.register(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        }

        assertThat(exception.message).isEqualTo("OIDC Verification Failed")
        then(userLookupContract).shouldHaveNoInteractions()
        then(userRegistrationContract).shouldHaveNoInteractions()
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("사용자 저장 시 예외가 발생하면 전파가 된다")
    fun register_fail_user_save() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willReturn(TEST_OIDC_USER_INFO)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND)).willThrow(RuntimeException("Persistence Failed"))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<RuntimeException> {
            registerService.register(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        }

        assertThat(exception.message).isEqualTo("Persistence Failed")
        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("토큰 발급 시 예외가 발생하면 전파가 된다")
    fun register_fail_token_issue() {
        // given
        given(oidcVerifyPort.verify(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)).willReturn(TEST_OIDC_USER_INFO)
        given(userRegistrationContract.register(TEST_REGISTER_COMMAND)).willReturn(TEST_USER_RESULT)
        given(tokenProviderPort.issue(any())).willThrow(RuntimeException("Token Issue Failed"))

        // when & then
        val exception = org.junit.jupiter.api.assertThrows<RuntimeException> {
            registerService.register(TEST_OAUTH_PROVIDER, TEST_ID_TOKEN)
        }

        assertThat(exception.message).isEqualTo("Token Issue Failed")
    }
}
