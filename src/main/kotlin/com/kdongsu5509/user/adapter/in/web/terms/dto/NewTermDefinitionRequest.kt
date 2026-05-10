package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import com.kdongsu5509.user.domain.terms.TermsTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class NewTermDefinitionRequest(
    @field:NotBlank(message = "약관 명칭은 필수입니다.")
    @field:Size(max = 100, message = "약관 명칭은 100자 이내여야 합니다.")
    val termsName: String,

    @field:NotNull(message = "약관 종류는 필수입니다.")
    val termsType: TermsTypes,

    @field:NotNull(message = "필수 약관 여부는 필수 입력 항목입니다.")
    val required: Boolean
)