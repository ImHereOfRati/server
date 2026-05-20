package com.kdongsu5509.user.application.port.`in`

import com.kdongsu5509.user.application.MultiTermsConsentCommand

interface UserAgreementUseCase {
    fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand)

    fun consent(email: String, id: Long)
}
