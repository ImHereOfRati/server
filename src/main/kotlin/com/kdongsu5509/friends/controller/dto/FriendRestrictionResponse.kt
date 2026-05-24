package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import java.time.LocalDateTime
import java.util.*

data class FriendRestrictionResponse(
    val id: UUID?,
    val restrictor: FriendRequestUserResponse,
    val restricted: FriendRequestUserResponse,
    val type: FriendRestrictionType,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val expiredAt: LocalDateTime?
) {
    companion object {
        fun fromDomain(domain: FriendRestriction) = FriendRestrictionResponse(
            id = domain.id,
            restrictor = FriendRequestUserResponse.from(domain.restrictor),
            restricted = FriendRequestUserResponse.from(domain.restricted),
            type = domain.type,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            expiredAt = domain.expiredAt
        )
    }
}
