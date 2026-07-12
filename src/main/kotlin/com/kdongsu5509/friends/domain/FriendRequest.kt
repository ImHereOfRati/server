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
    val message: RequestMessage,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    constructor(
        id: UUID? = null,
        requester: User,
        receiver: User,
        message: String,
        createdAt: LocalDateTime? = null,
        updatedAt: LocalDateTime? = null
    ) : this(id, requester, receiver, RequestMessage(message), createdAt, updatedAt)

    init {
        if (requester.id == receiver.id) FriendException.SELF_FRIENDSHIP.throwIt()
    }

    companion object {
        fun newRequest(requester: User, receiver: User, message: String): FriendRequest = FriendRequest(
            requester = requester,
            receiver = receiver,
            message = message
        )
    }

    fun requesterId() = requester.id
    fun receiverId() = receiver.id
    fun receiverEmail() = receiver.email

    fun isReceivedBy(email: String) = receiver.email == email
    fun isRequestedBy(email: String) = requester.email == email
    fun involves(email: String) = isReceivedBy(email) || isRequestedBy(email)

    fun accept(): Pair<Friendship, Friendship> {
        val requesterFriendship = Friendship(
            owner = requester,
            friend = receiver,
            friendAlias = FriendAlias.fromNickname(receiver.nickname)
        )
        val receiverFriendship = Friendship(
            owner = receiver,
            friend = requester,
            friendAlias = FriendAlias.fromNickname(requester.nickname)
        )
        return requesterFriendship to receiverFriendship
    }

    fun reject(): FriendRestriction = FriendRestriction.reject(restrictor = receiver, restricted = requester)
}
