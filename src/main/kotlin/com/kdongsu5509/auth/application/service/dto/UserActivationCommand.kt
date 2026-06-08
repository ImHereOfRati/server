package com.kdongsu5509.auth.application.service.dto

data class UserActivationCommand(
    val email: String,
    val consents: List<TermConsentCommand>
) {
    data class TermConsentCommand(
        val id: Long,
        val isAgreed: Boolean
    )
}
