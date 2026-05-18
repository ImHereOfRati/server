package com.kdongsu5509.auth.adapter.`in`.web.dto

import jakarta.validation.constraints.NotBlank

data class TokenRefreshRequest(
    @field:NotBlank(message = "리프레쉬 토큰은 필수입니다.")
    val refreshToken: String
)
