package com.kdongsu5509.user.controller.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.user.service.dto.UserResult
import java.util.*

data class CompactUserResponse(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oAuth2Provider: OAuth2Provider,
) {
    companion object {
        fun from(userResult: UserResult): CompactUserResponse {
            return CompactUserResponse(
                userResult.id,
                userResult.email,
                userResult.nickname,
                userResult.oauthProvider
            )
        }
    }
}
