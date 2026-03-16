package com.kdongsu5509.user.adapter.`in`.web.user.dto

import jakarta.validation.constraints.NotBlank

data class ReAuthenticationResponse(
    @field:NotBlank(message = "refreshToken?€ ?„ìˆ˜?…ë‹ˆ??)
    val refreshToken: String
)
