package com.kdongsu5509.agreement.repository.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataAgreementRepository : JpaRepository<AgreementJpaEntity, UUID> {
    @Query(
        """
        SELECT agreement
        FROM AgreementJpaEntity agreement
        WHERE agreement.userId = :userId
        ORDER BY agreement.occurredAt ASC
        """
    )
    fun findHistoryByUserId(@Param("userId") userId: UUID): List<AgreementJpaEntity>
}
