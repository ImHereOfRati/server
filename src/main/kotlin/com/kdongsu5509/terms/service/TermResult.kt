package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
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
        fun from(domain: Term): TermResult = TermResult(
            domain.id!!,
            domain.version,
            domain.type,
            domain.title,
            domain.content,
            domain.effectiveDate,
            domain.isRequired
        )
    }
}
