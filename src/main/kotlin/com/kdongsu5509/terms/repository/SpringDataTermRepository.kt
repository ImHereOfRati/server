package com.kdongsu5509.terms.repository

import com.kdongsu5509.terms.domain.TermTypes
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SpringDataTermRepository : JpaRepository<TermJpaEntity, Long> {
    fun findTopByTypeOrderByVersionDesc(type: TermTypes): TermJpaEntity?

    fun findAllByEffectiveDateLessThanEqual(effectiveDate: LocalDateTime): List<TermJpaEntity>
}
