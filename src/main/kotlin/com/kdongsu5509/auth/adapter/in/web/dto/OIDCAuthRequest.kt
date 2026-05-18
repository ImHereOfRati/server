package com.kdongsu5509.auth.adapter.`in`.web.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class OIDCAuthRequest(
    @field:NotNull(message = "OAuth2 제공자는 필수입니다.")
    val provider: OAuth2Provider,

    @field:NotBlank(message = "ID 토큰은 필수입니다.")
    val idToken: String,
)
