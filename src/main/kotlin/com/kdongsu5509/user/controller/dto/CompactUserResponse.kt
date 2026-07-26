package com.kdongsu5509.user.controller.dto

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.api.UserResult
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
                id = userResult.id,
                email = userResult.email,
                nickname = userResult.nickname,
                oAuth2Provider = userResult.oauthProvider
            )
        }
    }
}
