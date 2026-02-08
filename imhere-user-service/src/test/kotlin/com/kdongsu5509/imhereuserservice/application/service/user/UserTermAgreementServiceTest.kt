package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest.ConsentDetail
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserAgreementSavePort
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserUpdatePort
import com.kdongsu5509.imhereuserservice.domain.terms.TermDefinition
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UserTermAgreementServiceTest {

    @Mock
    lateinit var userUpdatePort: UserUpdatePort

    @Mock
    lateinit var termsDefinitionLoadPort: TermsDefinitionLoadPort

    @Mock
    lateinit var userAgreementSavePort: UserAgreementSavePort

    @InjectMocks
    lateinit var service: UserTermAgreementService

    private val username = "pending@kakao.com"

    @Test
    @DisplayName("모든 필수 약관에 동의하면 저장 후 유저를 활성화한다")
    fun consentAll_success() {
        // given
        val requiredTerms = listOf(
            TermDefinition(id = 1L, title = "필수1", termsTypes = TermsTypes.PRIVACY, isRequired = true),
            TermDefinition(id = 2L, title = "필수2", termsTypes = TermsTypes.LOCATION, isRequired = true)
        )
        given(termsDefinitionLoadPort.loadAllTerms()).willReturn(requiredTerms)

        val request = UserTermsConsentRequest(
            consents = listOf(
                ConsentDetail(1L, true),
                ConsentDetail(2L, true)
            )
        )

        // when
        service.consentAll(username, request)

        // then
        verify(userAgreementSavePort, times(1)).saveAgreements(username, listOf(1L, 2L))
        verify(userUpdatePort, times(1)).activate(username)
    }

    @Test
    @DisplayName("필수 약관 중 하나라도 동의하지 않으면 예외가 발생한다")
    fun consentAll_fail_required_missing() {
        // given
        val requiredTerms = listOf(
            TermDefinition(id = 1L, title = "필수1", termsTypes = TermsTypes.PRIVACY, isRequired = true)
        )
        given(termsDefinitionLoadPort.loadAllTerms()).willReturn(requiredTerms)

        val request = UserTermsConsentRequest(
            consents = listOf(
                ConsentDetail(1L, false)
            )
        )

        // when & then
        assertThatThrownBy {
            service.consentAll(username, request)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OBLIGATORY_TERM_NOT_AGREED)
    }

    @Test
    @DisplayName("단일 약관 동의 시 성공적으로 저장 로직을 호출한다")
    fun consent_single_success() {
        // given
        val termId = 100L

        // when
        service.consent(username, termId)

        // then
        verify(userAgreementSavePort).saveAgreement(username, termId)
    }
}