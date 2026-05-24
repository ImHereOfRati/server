package com.kdongsu5509.auth.application

data class UserActivationCommand(
    val email: String,
    val consents: List<RequiredTermConsentCommand>
) {
    data class RequiredTermConsentCommand(
        val id: Long,
        val isAgreed: Boolean
    )
}
