package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.domain.TermTypes
import java.time.LocalDateTime

data class TermCreateCommand(
    val type: TermTypes,
    val title: String,
    val content: String,
    var effectiveDate: LocalDateTime,
    var isRequired: Boolean
) {
    companion object {
        fun fromRequest(request: TermCreateRequest): TermCreateCommand {
            return TermCreateCommand(
                type = request.type,
                title = request.title,
                content = request.content,
                effectiveDate = request.effectiveDate,
                isRequired = request.isRequired
            )
        }
    }
}
