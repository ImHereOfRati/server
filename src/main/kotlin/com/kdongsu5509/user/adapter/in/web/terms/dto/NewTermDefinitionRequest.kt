package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import com.kdongsu5509.user.domain.terms.TermsTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class NewTermDefinitionRequest(
    @field:NotBlank(message = "?½ê? ?œëª©?€ ?„ìˆ˜?…ë‹ˆ??")
    @field:Size(max = 100, message = "?½ê? ?œëª©?€ 100???´ë‚´?¬ì•¼ ?©ë‹ˆ??")
    val termsName: String,

    @field:NotNull(message = "?½ê? ì¢…ë¥˜???„ìˆ˜?…ë‹ˆ??")
    var termsType: TermsTypes,

    @field:NotNull(message = "?„ìˆ˜ ?¬ë? ?¤ì •???„ë½?˜ì—ˆ?µë‹ˆ??")
    var isRequired: Boolean,
)
