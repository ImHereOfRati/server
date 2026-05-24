package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import java.util.*

/**
 * user 패키지의 CompactUserResponse와 동일한 스펙을 가지되,
 * friends 도메인의 독립성을 위해 완전히 분리한 유저 정보 응답 DTO
 */
data class FriendRequestUserResponse(
    val id: UUID,
    val email: String,
    val nickname: String,
    val oAuth2Provider: OAuth2Provider
) {
    companion object {
        fun from(user: User) = FriendRequestUserResponse(
            id = user.id!!,
            email = user.email,
            nickname = user.nickname,
            oAuth2Provider = user.oauthProvider
        )
    }
}
