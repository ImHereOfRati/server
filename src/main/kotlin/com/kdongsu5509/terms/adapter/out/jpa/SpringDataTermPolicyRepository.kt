package com.kdongsu5509.terms.adapter.out.jpa

import com.kdongsu5509.terms.domain.TermsTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataTermPolicyRepository : JpaRepository<TermPolicyJpaEntity, Long> {
    fun existsByTitleAndType(title: String, type: TermsTypes): Boolean
}
