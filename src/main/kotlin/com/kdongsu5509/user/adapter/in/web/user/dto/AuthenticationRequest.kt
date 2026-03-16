package com.kdongsu5509.user.adapter.`in`.web.user.dto

import com.kdongsu5509.user.domain.user.OAuth2Provider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AuthenticationRequest(
    @field:NotNull(message = "OAuth2 제공자는 필수입니다")
    val provider: OAuth2Provider,
    @field:NotBlank(message = "idToken은 필수입니다")
    val idToken: String,
)
