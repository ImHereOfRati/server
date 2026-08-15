package com.kdongsu5509.auth.adapter.`in`.web.dto

data class AdminChallengeResponse(
    val challenge: String,
    val expiresInSeconds: Long,
)

data class AdminTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)
