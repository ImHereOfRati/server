package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import com.kdongsu5509.user.domain.terms.TermDefinition
import com.kdongsu5509.user.domain.terms.TermsTypes

data class TermDefinitionResponse(
    var termDefinitionId: Long,
    var title: String,
    var termsTypes: TermsTypes,
    var isRequired: Boolean
) {
    companion object {
        fun from(domain: TermDefinition) = TermDefinitionResponse(
            termDefinitionId = domain.id,
            title = domain.title,
            termsTypes = domain.termsTypes,
            isRequired = domain.isRequired
        )
    }
}
