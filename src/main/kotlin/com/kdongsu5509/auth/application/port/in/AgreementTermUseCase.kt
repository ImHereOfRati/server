package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.user.adapter.`in`.web.dto.request.UserTermsConsentRequest

interface AgreementTermUseCase {
    fun consentAll(
        username: String,
        userTermsConsentRequest: UserTermsConsentRequest
    )

    fun consent(
        username: String,
        termDefinitionId: Long
    )
}
