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
)
