package com.kdongsu5509.user.api

import com.kdongsu5509.user.domain.OAuth2Provider

data class RegisterUserCommand(
    val email: String,
    val nickname: String,
    val oauthProvider: OAuth2Provider,
    val oidcSubject: String? = null,
)
