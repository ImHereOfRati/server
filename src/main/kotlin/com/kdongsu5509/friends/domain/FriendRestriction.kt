package com.kdongsu5509.friends.domain

import com.kdongsu5509.user.domain.User
import java.time.LocalDateTime
import java.util.*

data class FriendRestriction(
    val id: UUID? = null,
    val restrictor: User,
    val restricted: User,
    val type: FriendRestrictionType,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val expiredAt: LocalDateTime? = null
) {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean =
        expiredAt != null && !expiredAt.isAfter(now)

    fun isOwnedBy(email: String) = restrictor.email == email

    companion object {
        fun reject(restrictor: User, restricted: User): FriendRestriction {
            val now = LocalDateTime.now()
            return FriendRestriction(
                restrictor = restrictor,
                restricted = restricted,
                type = FriendRestrictionType.REJECT,
                expiredAt = FriendRestrictionType.REJECT.expiryFrom(now)
            )
        }

        fun block(restrictor: User, restricted: User): FriendRestriction = FriendRestriction(
            restrictor = restrictor,
            restricted = restricted,
            type = FriendRestrictionType.BLOCK,
            expiredAt = FriendRestrictionType.BLOCK.expiryFrom(LocalDateTime.now())
        )
    }
}
