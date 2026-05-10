package com.kdongsu5509.user.adapter.`in`.web.user.dto

import jakarta.validation.constraints.NotBlank

data class ReAuthenticationRequest(
    @field:NotBlank(message = "리프레시 토큰은 필수입니다.")
    val refreshToken: String
)
