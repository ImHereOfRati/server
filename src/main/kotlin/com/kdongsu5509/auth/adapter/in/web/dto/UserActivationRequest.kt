package com.kdongsu5509.auth.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.kdongsu5509.auth.application.UserActivationCommand
import com.kdongsu5509.auth.application.UserActivationCommand.RequiredTermConsentCommand
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class UserActivationRequest(
    @field:NotEmpty(message = "약관 동의 내역은 필수입니다.")
    @field:Valid
    val consents: List<TermConsent>
) {
    data class TermConsent(
        @field:NotNull(message = "약관 ID는 필수입니다.")
        val id: Long,

        @field:NotNull(message = "약관 동의 여부는 필수입니다.")
        @param:JsonProperty("agreed")
        val isAgreed: Boolean
    )

    fun toCommand(email: String): UserActivationCommand {
        return UserActivationCommand(
            email = email,
            consents = consents.map {
                RequiredTermConsentCommand(
                    id = it.id,
                    isAgreed = it.isAgreed
                )
            }
        )
    }
}
