package com.kdongsu5509.friends.domain

import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class Friendship(
    val id: UUID? = null,
    val owner: User,
    val friend: User,
    val friendAlias: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    fun updateFriendAlias(newAlias: String) = Friendship(
        id = id,
        owner = owner,
        friend = friend,
        friendAlias = newAlias,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
