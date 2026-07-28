package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.throwIt
import java.util.*

data class FriendPair(
    val low: UUID,
    val high: UUID
) {
    companion object {
        fun of(one: UUID, other: UUID): FriendPair {
            if (one == other) FriendException.SELF_FRIENDSHIP.throwIt()
            return if (one < other) FriendPair(one, other) else FriendPair(other, one)
        }

        fun restore(low: UUID, high: UUID): FriendPair = of(low, high)

        fun ordered(one: UUID, other: UUID): Pair<UUID, UUID> {
            if (one == other) FriendException.SELF_FRIENDSHIP.throwIt()

            if (one < other) return one to other
            return other to one
        }
    }

    fun contains(userId: UUID): Boolean = low == userId || high == userId

    fun memberOf(userId: UUID): UUID {
        if (low == userId) return low
        if (high == userId) return high

        FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
    }

    fun counterpartOf(userId: UUID): UUID {
        if (low == userId) return high
        if (high == userId) return low

        FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
    }

    fun isLow(userId: UUID): Boolean = low == userId
}
