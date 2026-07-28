package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.service.dto.FriendshipView
import java.time.LocalDateTime
import java.util.*

data class FriendshipResponse(
    val id: UUID? = null,
    val owner: FriendRequestUserResponse,
    val friend: FriendRequestUserResponse,
    val friendAlias: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    companion object {
        fun from(view: FriendshipView) = FriendshipResponse(
            id = view.id,
            owner = FriendRequestUserResponse.from(view.owner),
            friend = FriendRequestUserResponse.from(view.friend),
            friendAlias = view.friendAlias,
            createdAt = view.createdAt,
            updatedAt = view.updatedAt
        )
    }
}
