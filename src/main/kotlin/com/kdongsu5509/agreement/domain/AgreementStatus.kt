package com.kdongsu5509.agreement.domain

enum class AgreementStatus {
    CONSENT,
    WITHDRAW;

    companion object {
        fun next(
            currentAction: AgreementStatus?,
            requestedAgreed: Boolean,
        ): AgreementStatus? = when {
            currentAction == null && requestedAgreed -> CONSENT
            currentAction == CONSENT && !requestedAgreed -> WITHDRAW
            currentAction == WITHDRAW && requestedAgreed -> CONSENT
            else -> null
        }
    }
}
