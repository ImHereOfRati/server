package com.kdongsu5509.user.service.dto

data class MultiTermsConsentCommand(
    val consents: List<TermConsentCommand>
) {
    data class TermConsentCommand(
        val id: Long,
        val isAgreed: Boolean
    )
}
