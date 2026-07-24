package com.kdongsu5509.agreement.service.dto

import com.kdongsu5509.agreement.domain.Consent
import com.kdongsu5509.agreement.domain.ConsentItem

data class TermsConsentCommands(val consents: List<TermConsentCommand>) {
    data class TermConsentCommand(val id: Long, val isAgreed: Boolean)

    fun toConsent(): Consent {
        return Consent(
            consents.map {
                ConsentItem(it.id, it.isAgreed)
            }
        )
    }
}
