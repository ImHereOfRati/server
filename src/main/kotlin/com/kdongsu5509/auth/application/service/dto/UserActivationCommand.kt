package com.kdongsu5509.auth.application.service.dto

import java.util.UUID

data class UserActivationCommand(
    val userId: UUID,
    val email: String,
    val consents: List<TermConsentCommand>
) {
    data class TermConsentCommand(
        val id: Long,
        val isAgreed: Boolean
    )
}
