package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.UserActivationCommand
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.support.exception.type.ForbiddenException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserAgreementService
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import java.util.*

@ExtendWith(MockitoExtension::class)
class ActivateUserServiceTest {

    @Mock
    lateinit var userAgreementService: UserAgreementService

    @Mock
    lateinit var tokenProviderPort: ImHereTokenProviderPort

    @InjectMocks
    lateinit var activateUserService: ActivateUserService

    companion object {
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "tester"
        const val TEST_ACCESS_TOKEN = "access-token"
        const val TEST_REFRESH_TOKEN = "refresh-token"

        val TEST_USER_ID: UUID = UUID.randomUUID()
        val ACTIVE_USER = User(
            id = TEST_USER_ID,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE,
        )
    }

    @Test
    @DisplayName("약관 동의를 처리하고 활성화된 사용자 정보로 JWT 토큰을 발급한다")
    fun activate_success() {
        // given
        val command = UserActivationCommand(
            email = TEST_EMAIL,
            consents = listOf(
                UserActivationCommand.TermConsentCommand(id = 1L, isAgreed = true),
                UserActivationCommand.TermConsentCommand(id = 2L, isAgreed = false),
            )
        )
        val consentsCommand = MultiTermsConsentCommand(
            consents = listOf(
                MultiTermsConsentCommand.TermConsentCommand(id = 1L, isAgreed = true),
                MultiTermsConsentCommand.TermConsentCommand(id = 2L, isAgreed = false),
            )
        )
        val expectedClaims = JwtTokenClaims.fromUser(ACTIVE_USER)
        val expectedToken = ImHereJwtToken(TEST_ACCESS_TOKEN, TEST_REFRESH_TOKEN)

        given(userAgreementService.consentAll(TEST_EMAIL, consentsCommand)).willReturn(ACTIVE_USER)
        given(tokenProviderPort.issue(expectedClaims)).willReturn(expectedToken)

        // when
        val result = activateUserService.activate(command, UserStatus.PENDING.name)

        // then
        assertThat(result).isEqualTo(expectedToken)
        then(userAgreementService).should().consentAll(TEST_EMAIL, consentsCommand)
        then(tokenProviderPort).should().issue(expectedClaims)
    }

    @Test
    @DisplayName("약관 동의 처리 중 예외가 발생하면 토큰을 발급하지 않고 예외를 전파한다")
    fun activate_fail_already_active() {
        // given
        val command = UserActivationCommand(
            email = TEST_EMAIL,
            consents = listOf(UserActivationCommand.TermConsentCommand(id = 1L, isAgreed = true))
        )

        // when & then
        assertThatThrownBy { activateUserService.activate(command, UserStatus.ACTIVE.name) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    @DisplayName("약관 동의 처리 중 예외가 발생하면 토큰을 발급하지 않고 예외를 전파한다")
    fun activate_fail_consent_all() {
        // given
        val command = UserActivationCommand(
            email = TEST_EMAIL,
            consents = listOf(UserActivationCommand.TermConsentCommand(id = 1L, isAgreed = true))
        )
        val consentsCommand = MultiTermsConsentCommand(
            consents = listOf(MultiTermsConsentCommand.TermConsentCommand(id = 1L, isAgreed = true))
        )
        val expectedException = RuntimeException("Consent Failed")

        given(userAgreementService.consentAll(TEST_EMAIL, consentsCommand)).willThrow(expectedException)

        // when & then
        assertThatThrownBy { activateUserService.activate(command, UserStatus.PENDING.name) }
            .isSameAs(expectedException)

        then(tokenProviderPort).shouldHaveNoInteractions()
    }

    @Test
    @DisplayName("토큰 발급 중 예외가 발생하면 예외를 전파한다")
    fun activate_fail_token_issue() {
        // given
        val command = UserActivationCommand(
            email = TEST_EMAIL,
            consents = listOf(UserActivationCommand.TermConsentCommand(id = 1L, isAgreed = true))
        )
        val consentsCommand = MultiTermsConsentCommand(
            consents = listOf(MultiTermsConsentCommand.TermConsentCommand(id = 1L, isAgreed = true))
        )
        val expectedClaims = JwtTokenClaims.fromUser(ACTIVE_USER)
        val expectedException = RuntimeException("Token Issue Failed")

        given(userAgreementService.consentAll(TEST_EMAIL, consentsCommand)).willReturn(ACTIVE_USER)
        given(tokenProviderPort.issue(expectedClaims)).willThrow(expectedException)

        // when & then
        assertThatThrownBy { activateUserService.activate(command, UserStatus.PENDING.name) }
            .isSameAs(expectedException)

        then(userAgreementService).should().consentAll(TEST_EMAIL, consentsCommand)
    }
}
