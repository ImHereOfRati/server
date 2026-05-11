package com.kdongsu5509.user.adapter.`in`.web.user.dto.response

import com.kdongsu5509.user.application.dto.AuthenticationProcessResult
import com.kdongsu5509.user.application.dto.ImHereJwt

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
) {
    companion object {
        fun fromAuthenticationProcessResult(authenticationProcessResult: AuthenticationProcessResult): AuthenticationResponse {
            return AuthenticationResponse(
                authenticationProcessResult.accessToken,
                authenticationProcessResult.refreshToken
            )
        }

        fun fromImHereJwt(imhereJwt: ImHereJwt): AuthenticationResponse {
            return AuthenticationResponse(
                imhereJwt.accessToken,
                imhereJwt.refreshToken
            )
        }
    }
}
