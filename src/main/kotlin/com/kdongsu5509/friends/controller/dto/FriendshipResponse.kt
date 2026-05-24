package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.domain.Friendship
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
        fun from(domain: Friendship) = FriendshipResponse(
            id = domain.id,
            owner = FriendRequestUserResponse.from(domain.owner),
            friend = FriendRequestUserResponse.from(domain.friend),
            friendAlias = domain.friendAlias,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt
        )
    }
}
