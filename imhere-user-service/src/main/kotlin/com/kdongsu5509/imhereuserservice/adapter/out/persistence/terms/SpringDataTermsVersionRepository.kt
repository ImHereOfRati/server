package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface SpringDataTermsVersionRepository : JpaRepository<TermsVersionJpaEntity, UUID> {
}