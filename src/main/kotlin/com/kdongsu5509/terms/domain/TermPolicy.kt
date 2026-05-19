package com.kdongsu5509.terms.domain

data class TermPolicy(
    val id: Long,
    val title: String,
    val type: TermsTypes,
    val isRequired: Boolean,
)
