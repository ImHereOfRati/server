package com.kdongsu5509.terms.domain

import java.time.LocalDateTime

data class Term(
    val id: Long?,
    val version: Long,
    val type: TermTypes,
    val title: String,
    val content: String,
    val effectiveDate: LocalDateTime,
    val isRequired: Boolean,
) {
    companion object {
        fun createWithVersion(
            type: TermTypes,
            title: String,
            content: String,
            effectiveDate: LocalDateTime,
            isRequired: Boolean,
            version: Long
        ): Term {
            return Term(
                id = null,
                version = version,
                type = type,
                title = title,
                content = content,
                effectiveDate = effectiveDate,
                isRequired = isRequired
            )
        }
    }
}
