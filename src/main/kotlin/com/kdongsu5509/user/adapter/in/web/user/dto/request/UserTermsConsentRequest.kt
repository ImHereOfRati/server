package com.kdongsu5509.user.adapter.`in`.web.user.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class UserTermsConsentRequest(
    @field:NotEmpty(message = "약관 동의 내역은 필수입니다.")
    val consents: List<ConsentDetail>
) {
    data class ConsentDetail(
        @field:NotNull(message = "약관 정의 ID는 필수입니다.")
        val termDefinitionId: Long,
        
        @field:NotNull(message = "약관 동의 여부는 필수입니다.")
        @param:JsonProperty("agreed")
        val isAgreed: Boolean
    )
}
