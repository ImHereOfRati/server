package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.TermTypes
import java.time.LocalDateTime

data class TermCreateCommand(
    val type: TermTypes,
    val title: String,
    val content: String,
    var effectiveDate: LocalDateTime,
    var isRequired: Boolean
)
