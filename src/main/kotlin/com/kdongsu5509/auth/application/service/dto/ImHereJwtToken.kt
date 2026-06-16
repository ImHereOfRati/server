package com.kdongsu5509.auth.application.service.dto

data class ImHereJwtToken(
    val accessToken: String,
    val refreshToken: String,
    val userStatus: String? = null
)
