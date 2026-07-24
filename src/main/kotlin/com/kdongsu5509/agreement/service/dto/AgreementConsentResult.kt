package com.kdongsu5509.agreement.service.dto

import com.kdongsu5509.agreement.AgreementException
import com.kdongsu5509.support.exception.throwIt

data class AgreementConsentResult(
    val requiredAgreementsSatisfied: Boolean,
) {
    fun requireRequiredAgreementsSatisfied() {
        if (!requiredAgreementsSatisfied) {
            AgreementException.REQUIRED_AGREEMENTS_NOT_SATISFIED.throwIt()
        }
    }
}
