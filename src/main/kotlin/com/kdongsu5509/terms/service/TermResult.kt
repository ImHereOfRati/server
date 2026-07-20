package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.TermJpaEntity
import java.time.LocalDateTime

data class TermResult(
    val id: Long,
    val version: Long,
    val type: TermTypes,
    val title: String,
    val content: String,
    val effectiveDate: LocalDateTime,
    val isRequired: Boolean
) {
    companion object {
        fun from(entity: TermJpaEntity): TermResult = TermResult(
            entity.id!!,
            entity.version,
            entity.type,
            entity.title,
            entity.content,
            entity.effectiveDate,
            entity.isRequired
        )
    }
}
