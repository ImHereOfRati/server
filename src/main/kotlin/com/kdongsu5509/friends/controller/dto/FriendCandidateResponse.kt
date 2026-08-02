package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import java.util.*

data class FriendCandidateResponse(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oAuth2Provider: OAuth2Provider,
) {
    companion object {
        fun from(userResult: UserResult): FriendCandidateResponse = FriendCandidateResponse(
            id = userResult.id,
            email = userResult.email,
            nickname = userResult.nickname,
            oAuth2Provider = userResult.oauthProvider,
        )
    }
}
