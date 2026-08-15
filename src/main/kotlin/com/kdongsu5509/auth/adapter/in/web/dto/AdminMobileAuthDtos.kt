package com.kdongsu5509.auth.adapter.`in`.web.dto

import jakarta.validation.constraints.NotBlank

data class AdminMobileLoginRequest(
    @field:NotBlank val adminId: String,
    @field:NotBlank val password: String,
)

data class AdminMobileMfaRequest(
    @field:NotBlank val challenge: String,
    @field:NotBlank val code: String,
)

data class AdminMobileRefreshRequest(
    @field:NotBlank val refreshToken: String,
)

data class AdminMobileChallengeResponse(
    val challenge: String,
    val expiresInSeconds: Long,
)

data class AdminMobileTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)

data class AdminMobileSessionResponse(
    val adminId: String,
    val nickname: String,
    val role: String,
)
