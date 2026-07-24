package com.kdongsu5509.agreement.repository

import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.domain.ConsentChange
import com.kdongsu5509.agreement.repository.jpa.AgreementJpaEntity
import com.kdongsu5509.agreement.repository.jpa.SpringDataAgreementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.util.*

@ExtendWith(MockitoExtension::class)
class AgreementRepositoryTest {
    @Mock
    lateinit var springDataRepository: SpringDataAgreementRepository

    @InjectMocks
    lateinit var repository: AgreementRepository

    @Test
    @DisplayName("사용자의 약관 동의 상태 변경 내역을 모두 저장한다")
    fun recordChanges_success_saves_every_state_transition() {
        // given
        val userId = UUID.randomUUID()
        val consentChanges = listOf(
            ConsentChange(1L, AgreementStatus.CONSENT),
            ConsentChange(2L, AgreementStatus.WITHDRAW),
        )

        // when
        repository.recordChanges(userId, consentChanges)

        // then
        val agreementEntitiesCaptor = argumentCaptor<List<AgreementJpaEntity>>()
        verify(springDataRepository).saveAll(agreementEntitiesCaptor.capture())
        val savedAgreementEntities = agreementEntitiesCaptor.firstValue
        assertThat(savedAgreementEntities.map { it.userId }).containsOnly(userId)
        assertThat(savedAgreementEntities.map { it.termId to it.action }).containsExactly(
            1L to AgreementStatus.CONSENT,
            2L to AgreementStatus.WITHDRAW,
        )
    }

    @Test
    @DisplayName("사용자의 저장된 약관 동의 변경 이력을 조회한다")
    fun findHistory_success_returns_saved_agreement_history() {
        // given
        val userId = UUID.randomUUID()
        val consentAgreement = AgreementJpaEntity(userId, 1L, AgreementStatus.CONSENT)
        val withdrawnAgreement = AgreementJpaEntity(userId, 1L, AgreementStatus.WITHDRAW)
        given(springDataRepository.findHistoryByUserId(userId))
            .willReturn(listOf(consentAgreement, withdrawnAgreement))

        // when
        val agreementHistory = repository.findHistory(userId)

        // then
        assertThat(agreementHistory)
            .containsExactly(consentAgreement, withdrawnAgreement)
    }
}
