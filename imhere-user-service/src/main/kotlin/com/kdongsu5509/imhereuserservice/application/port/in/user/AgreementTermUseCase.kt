package com.kdongsu5509.imhereuserservice.application.port.`in`.user

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest

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