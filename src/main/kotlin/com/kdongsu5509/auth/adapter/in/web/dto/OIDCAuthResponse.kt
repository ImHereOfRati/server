package com.kdongsu5509.auth.adapter.`in`.web.dto

import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.user.domain.UserStatus

data class OIDCAuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userStatus: String? = null
) {
    companion object {
        fun fromImHereJwtToken(imhereJwtToken: ImHereJwtToken, userStatus: UserStatus? = null): OIDCAuthResponse {
            return OIDCAuthResponse(
                imhereJwtToken.accessToken,
                imhereJwtToken.refreshToken,
                userStatus?.toString() ?: imhereJwtToken.userStatus
            )
        }
    }
}
