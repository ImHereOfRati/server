package com.kdongsu5509.user.adapter.`in`.web.user.dto

import com.kdongsu5509.user.domain.user.OAuth2Provider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AuthenticationRequest(
    @field:NotNull(message = "OAuth2 ?œê³µ?ëŠ” ?„ìˆ˜?…ë‹ˆ??)
    var provider: OAuth2Provider,
    @field:NotBlank(message = "idToken?€ ?„ìˆ˜?…ë‹ˆ??")
    val idToken: String,
)
