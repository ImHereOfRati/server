package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.service.dto.FriendMember
import java.util.*

data class FriendRequestUserResponse(
    val id: UUID,
    val email: String,
    val nickname: String
) {
    companion object {
        fun from(member: FriendMember) = FriendRequestUserResponse(
            id = member.id,
            email = member.email,
            nickname = member.nickname
        )
    }
}
