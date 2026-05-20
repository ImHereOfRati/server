package com.kdongsu5509.user.controller.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.user.service.dto.UserResult
import java.util.*

data class DetailUserResponse(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oAuth2Provider: OAuth2Provider,
    val role: UserRole,
    val status: UserStatus
) {
    companion object {
        fun from(userResult: UserResult): DetailUserResponse {
            return DetailUserResponse(
                id = userResult.id,
                email = userResult.email,
                nickname = userResult.nickname,
                oAuth2Provider = userResult.oauthProvider,
                role = userResult.role,
                status = userResult.status
            )
        }
    }
}
