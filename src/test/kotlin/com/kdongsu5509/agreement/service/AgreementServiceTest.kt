package com.kdongsu5509.agreement.service

import com.kdongsu5509.agreement.AgreementException
import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.domain.ConsentChange
import com.kdongsu5509.agreement.repository.AgreementRepository
import com.kdongsu5509.agreement.repository.jpa.AgreementJpaEntity
import com.kdongsu5509.agreement.service.dto.TermsConsentCommands
import com.kdongsu5509.terms.TermCatalog
import com.kdongsu5509.terms.TermFact
import com.kdongsu5509.user.api.UserActivationContract
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.util.*

@ExtendWith(MockitoExtension::class)
class AgreementServiceTest {

    companion object {
        val USER_ID: UUID = UUID.randomUUID()
        val EFFECTIVE_TERMS = listOf(
            TermFact(1L, 1L, "SERVICE", true),
            TermFact(2L, 1L, "PRIVACY", true),
            TermFact(3L, 1L, "MARKETING", false),
        )
    }

    @Mock
    lateinit var userActivationContract: UserActivationContract

    @Mock
    lateinit var agreementRepository: AgreementRepository

    @Mock
    lateinit var termCatalog: TermCatalog

    @InjectMocks
    lateinit var agreementService: AgreementService

    @Test
    @DisplayName("변경된 동의 상태만 저장하고 필수 약관이 충족되면 사용자를 활성화한다")
    fun consent_success_records_transitions_and_activates_user() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(EFFECTIVE_TERMS)
        given(agreementRepository.findHistory(USER_ID))
            .willReturn(
                history(2L to AgreementStatus.CONSENT)
            )
        val consentCommands = command(1L to true, 2L to true, 3L to false)

        // when
        val consentResult = agreementService.consent(USER_ID, consentCommands)

        // then
        assertThat(consentResult.requiredAgreementsSatisfied).isTrue()
        verify(agreementRepository).recordChanges(
            USER_ID,
            listOf(ConsentChange(1L, AgreementStatus.CONSENT))
        )
        verify(userActivationContract).activateIfPending(USER_ID)
    }

    @Test
    @DisplayName("기존 필수 약관 동의를 철회하면 미동의 상태를 반환하고 사용자를 활성화하지 않는다")
    fun consent_success_withdraws_existing_agreement_without_activation() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(EFFECTIVE_TERMS)
        given(agreementRepository.findHistory(USER_ID)).willReturn(
            history(1L to AgreementStatus.CONSENT, 2L to AgreementStatus.CONSENT)
        )
        val consentCommands = command(1L to false)

        // when
        val consentResult = agreementService.consent(USER_ID, consentCommands)

        // then
        assertThat(consentResult.requiredAgreementsSatisfied).isFalse()
        verify(agreementRepository).recordChanges(
            USER_ID,
            listOf(ConsentChange(1L, AgreementStatus.WITHDRAW))
        )
        verify(userActivationContract, never()).activateIfPending(any())
    }

    @Test
    @DisplayName("요청한 동의 상태가 현재와 같으면 변경 내역을 추가하지 않는다")
    fun consent_success_skips_unchanged_agreement_state() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(EFFECTIVE_TERMS)
        given(agreementRepository.findHistory(USER_ID)).willReturn(
            history(1L to AgreementStatus.CONSENT, 2L to AgreementStatus.CONSENT)
        )
        val consentCommands = command(1L to true)

        // when
        agreementService.consent(USER_ID, consentCommands)

        // then
        verify(agreementRepository, never()).recordChanges(any(), any())
        verify(userActivationContract).activateIfPending(USER_ID)
    }

    @Test
    @DisplayName("갱신 약관에 동의하면 동의 내역을 저장하고 사용자를 활성화하지 않는다")
    fun consentToRenewedTerm_success_records_consent_without_activation() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(
            listOf(TermFact(2L, 2L, "SERVICE", true))
        )
        given(agreementRepository.findHistory(USER_ID)).willReturn(
            history(1L to AgreementStatus.CONSENT)
        )
        given(termCatalog.findTermFacts(setOf(1L))).willReturn(
            listOf(TermFact(1L, 1L, "SERVICE", true))
        )

        // when
        agreementService.consentToRenewedTerm(USER_ID, 2L)

        // then
        verify(agreementRepository).recordChanges(
            USER_ID,
            listOf(ConsentChange(2L, AgreementStatus.CONSENT))
        )
        verify(userActivationContract, never()).activateIfPending(any())
    }

    @Test
    @DisplayName("동의한 선택 약관을 철회하면 철회 내역을 저장한다")
    fun withdraw_success_records_withdrawal_for_consented_optional_agreement() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(
            listOf(TermFact(2L, 2L, "SERVICE", false))
        )
        given(agreementRepository.findHistory(USER_ID)).willReturn(
            history(2L to AgreementStatus.CONSENT)
        )

        // when
        agreementService.withdraw(USER_ID, 2L)

        // then
        verify(agreementRepository).recordChanges(
            USER_ID,
            listOf(ConsentChange(2L, AgreementStatus.WITHDRAW))
        )
        verify(userActivationContract, never()).activateIfPending(any())
    }

    @Test
    @DisplayName("필수 약관의 동의를 철회하면 예외가 발생한다")
    fun withdraw_fail_required_agreement_cannot_be_withdrawn() {
        // given
        given(termCatalog.findEffectiveTermFacts()).willReturn(
            listOf(TermFact(2L, 2L, "SERVICE", true))
        )
        given(agreementRepository.findHistory(USER_ID)).willReturn(
            history(2L to AgreementStatus.CONSENT)
        )

        // when & then
        assertThatThrownBy { agreementService.withdraw(USER_ID, 2L) }
            .extracting("errorCode")
            .isEqualTo(AgreementException.REQUIRED_AGREEMENT_CANNOT_BE_WITHDRAWN)

        verify(agreementRepository, never()).recordChanges(any(), any())
    }

    private fun command(vararg items: Pair<Long, Boolean>) = TermsConsentCommands(
        items.map { TermsConsentCommands.TermConsentCommand(it.first, it.second) }
    )

    private fun history(vararg statuses: Pair<Long, AgreementStatus>): List<AgreementJpaEntity> =
        statuses.map { (termId, status) ->
            AgreementJpaEntity(USER_ID, termId, status)
        }
}
