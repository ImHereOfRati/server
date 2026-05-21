package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class FriendRequest(
    val id: UUID? = null,
    val requester: User,
    val receiver: User,
    val message: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    init {
        if (requester.id == receiver.id) FriendException.SELF_FRIENDSHIP.throwIt()
    }
}
