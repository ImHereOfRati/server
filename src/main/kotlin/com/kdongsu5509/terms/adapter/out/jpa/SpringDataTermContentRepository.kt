package com.kdongsu5509.terms.adapter.out.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SpringDataTermContentRepository : JpaRepository<TermContentJpaEntity, Long> {
    @Query("SELECT c FROM TermContentJpaEntity c WHERE c.termPolicy.id = :policyId AND c.isActive = true")
    fun findCurrentByPolicyId(policyId: Long): TermContentJpaEntity?

    @Query("SELECT c FROM TermContentJpaEntity c WHERE c.termPolicy.id IN :policyIds AND c.isActive = true")
    fun findAllCurrentByPolicyIds(policyIds: List<Long>): List<TermContentJpaEntity>
}
