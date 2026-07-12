package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class Friendship(
    val id: UUID? = null,
    val owner: User,
    val friend: User,
    val friendAlias: FriendAlias,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    constructor(
        id: UUID? = null,
        owner: User,
        friend: User,
        friendAlias: String,
        createdAt: LocalDateTime? = null,
        updatedAt: LocalDateTime? = null
    ) : this(id, owner, friend, FriendAlias(friendAlias), createdAt, updatedAt)

    init {
        if (owner.id == friend.id) FriendException.SELF_FRIENDSHIP.throwIt()
    }

    fun updateFriendAlias(newAlias: String) = copy(friendAlias = FriendAlias(newAlias))

    fun isOwnedBy(email: String) = owner.email == email
    fun ownerId(): UUID = owner.id!!
    fun friendId(): UUID = friend.id!!
}
