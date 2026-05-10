package com.kdongsu5509.user.application.dto

data class AuthenticationProcessResult(
    val isNewUser: Boolean,
    val accessToken: String,
    val refreshToken: String
)
