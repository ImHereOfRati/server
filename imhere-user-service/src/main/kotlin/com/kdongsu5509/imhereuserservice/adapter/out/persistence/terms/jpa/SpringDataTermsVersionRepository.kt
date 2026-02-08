package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface SpringDataTermsVersionRepository : JpaRepository<TermsVersionJpaEntity, Long> {
    @Query(
        """
    SELECT v FROM TermsVersionJpaEntity v 
    WHERE v.terms.id = :termsDefinitionId
      AND v.isActive = true 
"""
    )
    fun findActiveVersion(termsDefinitionId: Long): Optional<TermsVersionJpaEntity>

    @Query("SELECT tv FROM TermsVersionJpaEntity tv WHERE tv.terms.id IN :termDefinitionIds AND tv.isActive = true")
    fun findAllActiveByDefinitionIds(termDefinitionIds: List<Long>): List<TermsVersionJpaEntity>
}