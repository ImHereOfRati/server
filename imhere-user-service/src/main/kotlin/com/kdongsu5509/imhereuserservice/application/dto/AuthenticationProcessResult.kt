package com.kdongsu5509.imhereuserservice.application.dto

data class AuthenticationProcessResult(
    val statusCode: Int,
    val accessToken: String,
    val refreshToken: String
)