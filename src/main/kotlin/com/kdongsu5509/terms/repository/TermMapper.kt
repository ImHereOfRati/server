package com.kdongsu5509.terms.repository

import com.kdongsu5509.terms.domain.Term
import org.springframework.stereotype.Component

@Component
class TermMapper {
    fun toDomain(entity: TermJpaEntity?): Term? {
        if (entity == null) {
            return null
        }
        return Term(
            id = entity.id,
            version = entity.version,
            type = entity.type,
            title = entity.title,
            content = entity.content,
            effectiveDate = entity.effectiveDate,
            isRequired = entity.isRequired
        )
    }

    fun toEntity(term: Term): TermJpaEntity {
        return TermJpaEntity(
            id = term.id,
            version = term.version,
            type = term.type,
            title = term.title,
            content = term.content,
            effectiveDate = term.effectiveDate,
            isRequired = term.isRequired
        )
    }
}
