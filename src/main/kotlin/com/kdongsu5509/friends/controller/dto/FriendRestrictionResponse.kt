package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.service.dto.FriendRestrictionType
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
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
        fun fromDomain(view: FriendRestrictionView) = FriendRestrictionResponse(
            id = view.id,
            restrictor = FriendRequestUserResponse.from(view.restrictor),
            restricted = FriendRequestUserResponse.from(view.restricted),
            type = view.type,
            createdAt = view.createdAt,
            updatedAt = view.updatedAt,
            expiredAt = view.expiredAt
        )
    }
}
