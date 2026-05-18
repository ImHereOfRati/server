package com.kdongsu5509.auth.application

data class ImHereJwtToken(
    val accessToken: String,
    val refreshToken: String
)
