package com.kdongsu5509.agreement.controller.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kdongsu5509.agreement.service.dto.TermsConsentCommands
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class AgreementConsentRequest(
    @field:NotEmpty(message = "약관 동의 변경 내역은 필수입니다.")
    @field:Valid
    val consents: List<TermConsent>,
) {
    data class TermConsent(
        @field:NotNull(message = "약관 ID는 필수입니다.")
        val id: Long,
        @field:NotNull(message = "약관 동의 여부는 필수입니다.")
        @param:JsonProperty("agreed")
        val isAgreed: Boolean,
    )

    fun toCommand() = TermsConsentCommands(
        consents.map { TermsConsentCommands.TermConsentCommand(it.id, it.isAgreed) }
    )
}
