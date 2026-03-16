package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import com.kdongsu5509.user.domain.terms.TermVersion
import java.time.LocalDateTime

data class TermVersionResponse(
    val version: String,
    val content: String,
    val effectiveDate: LocalDateTime,
) {
    companion object {
        fun from(domain: TermVersion) = TermVersionResponse(
            version = domain.version,
            content = domain.content,
            effectiveDate = domain.effectiveDate,
        )
    }
}
