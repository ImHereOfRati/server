package com.kdongsu5509.agreement.repository

import com.kdongsu5509.agreement.domain.ConsentChange
import com.kdongsu5509.agreement.repository.jpa.AgreementJpaEntity
import com.kdongsu5509.agreement.repository.jpa.SpringDataAgreementRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class AgreementRepository(
    private val agreementRepository: SpringDataAgreementRepository,
) {
    fun recordChanges(userId: UUID, changes: List<ConsentChange>) {
        agreementRepository.saveAll(
            changes.map { AgreementJpaEntity(userId, it.termId, it.action) }
        )
    }

    fun findHistory(userId: UUID): List<AgreementJpaEntity> =
        agreementRepository.findHistoryByUserId(userId)
}
