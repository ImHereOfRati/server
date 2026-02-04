package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms

import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataTermsDefinitionRepository : JpaRepository<TermsDefinitionJpaEntity, Long> {
    fun existsByTermsTitleAndTermsType(
        termsTitle: String,
        termsType: TermsTypes
    ): Boolean
}