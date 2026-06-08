package com.kdongsu5509.auth.adapter.`in`.web.dto

import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken

data class OIDCAuthResponse(
    val accessToken: String,
    val refreshToken: String
) {
    companion object {
        fun fromImHereJwtToken(imhereJwtToken: ImHereJwtToken): OIDCAuthResponse {
            return OIDCAuthResponse(imhereJwtToken.accessToken, imhereJwtToken.refreshToken)
        }
    }
}
