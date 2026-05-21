package com.kdongsu5509.friends.domain

import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class FriendRelationship(
    val id: UUID? = null,
    val owner: User,
    val friend: User,
    val friendAlias: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
)
