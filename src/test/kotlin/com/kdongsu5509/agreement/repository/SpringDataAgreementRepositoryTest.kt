package com.kdongsu5509.agreement.repository

import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.repository.jpa.AgreementJpaEntity
import com.kdongsu5509.agreement.repository.jpa.SpringDataAgreementRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.TermJpaEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class SpringDataAgreementRepositoryTest @Autowired constructor(
    private val agreementRepository: SpringDataAgreementRepository,
    private val entityManager: EntityManager,
) {
    @Test
    @DisplayName("사용자의 약관 동의 내역을 저장하고 식별자로 조회한다")
    fun save_success_persists_and_finds_agreement() {
        // given
        val savedUser = UserJpaEntity(
            email = "user@example.com",
            nickname = "user",
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE,
        )
        entityManager.persist(savedUser)
        val savedTerm = TermJpaEntity(
            id = null,
            version = 1L,
            type = TermTypes.SERVICE,
            title = "Service Terms",
            content = "Content",
            effectiveDate = LocalDateTime.now(),
            isRequired = true,
        )
        entityManager.persist(savedTerm)

        // when
        val savedAgreement = agreementRepository.save(
            AgreementJpaEntity(savedUser.id!!, savedTerm.id!!, AgreementStatus.CONSENT)
        )
        entityManager.flush()
        entityManager.clear()
        val foundAgreement = agreementRepository.findById(savedAgreement.id!!).orElseThrow()

        // then
        assertThat(foundAgreement.userId).isEqualTo(savedUser.id)
        assertThat(foundAgreement.termId).isEqualTo(savedTerm.id)
        assertThat(foundAgreement.action).isEqualTo(AgreementStatus.CONSENT)
        assertThat(foundAgreement.occurredAt).isNotNull()
    }
}
