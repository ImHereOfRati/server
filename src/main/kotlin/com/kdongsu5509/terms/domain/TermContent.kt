package com.kdongsu5509.terms.domain

import java.time.LocalDateTime

data class TermContent(
    val id: Long,
    val version: String,
    val content: String,
    val effectiveDate: LocalDateTime
)
