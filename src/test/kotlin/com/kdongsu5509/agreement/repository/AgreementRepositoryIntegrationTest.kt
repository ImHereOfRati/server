package com.kdongsu5509.agreement.repository

import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.domain.ConsentChange
import com.kdongsu5509.agreement.repository.jpa.SpringDataAgreementRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import com.kdongsu5509.terms.repository.TermJpaEntity
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Import(AgreementRepository::class)
class AgreementRepositoryIntegrationTest @Autowired constructor(
    private val repository: AgreementRepository,
    private val userRepository: SpringDataUserRepository,
    private val termRepository: SpringDataTermRepository,
    private val agreementRepository: SpringDataAgreementRepository,
    private val entityManager: EntityManager,
) {
    @Test
    @DisplayName("동의 철회 후 재동의해도 모든 약관 동의 변경 이력을 보존한다")
    fun recordChanges_success_preserves_consent_withdrawal_and_reconsent_history() {
        // given
        val savedUser = userRepository.save(
            UserJpaEntity(
                email = "user@example.com",
                nickname = "user",
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.PENDING,
            )
        )
        val savedRequiredTerm = termRepository.save(
            TermJpaEntity(
                id = null,
                version = 1L,
                type = TermTypes.SERVICE,
                title = "Service Terms",
                content = "Content",
                effectiveDate = LocalDateTime.now(),
                isRequired = true,
            )
        )
        entityManager.flush()

        // when
        repository.recordChanges(
            savedUser.id!!,
            listOf(ConsentChange(savedRequiredTerm.id!!, AgreementStatus.CONSENT))
        )
        repository.recordChanges(
            savedUser.id!!,
            listOf(ConsentChange(savedRequiredTerm.id!!, AgreementStatus.WITHDRAW))
        )
        repository.recordChanges(
            savedUser.id!!,
            listOf(ConsentChange(savedRequiredTerm.id!!, AgreementStatus.CONSENT))
        )
        entityManager.flush()
        entityManager.clear()

        // then
        val agreementHistory = agreementRepository.findHistoryByUserId(savedUser.id!!)
        assertThat(agreementHistory).hasSize(3)
        assertThat(agreementHistory.map { it.action }).containsExactly(
            AgreementStatus.CONSENT,
            AgreementStatus.WITHDRAW,
            AgreementStatus.CONSENT,
        )
    }
}
