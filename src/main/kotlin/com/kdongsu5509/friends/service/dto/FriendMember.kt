package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.user.api.UserResult
import java.util.*

data class FriendMember(
    val id: UUID,
    val email: String,
    val nickname: String,
) {
    companion object {
        fun from(user: UserResult) = FriendMember(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
        )
    }
}
