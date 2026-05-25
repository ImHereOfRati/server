package com.kdongsu5509.terms.controller.dto

import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import java.time.LocalDateTime

data class TermResponse(
    val id: Long?,
    val version: Long,
    val type: TermTypes,
    val title: String,
    val content: String,
    val effectiveDate: LocalDateTime,
    val isRequired: Boolean
) {
    companion object {
        fun from(result: TermResult) = TermResponse(
            result.id,
            result.version,
            result.type,
            result.title,
            result.content,
            result.effectiveDate,
            result.isRequired
        )
    }
}
