package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.domain.FriendRequest
import java.time.LocalDateTime
import java.util.*

data class FriendRequestResponse(
    val id: UUID,
    val requester: FriendRequestUserResponse,
    val receiver: FriendRequestUserResponse,
    val message: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(domain: FriendRequest) = FriendRequestResponse(
            id = domain.id!!,
            requester = FriendRequestUserResponse.from(domain.requester),
            receiver = FriendRequestUserResponse.from(domain.receiver),
            message = domain.message,
            createdAt = domain.createdAt!!,
            updatedAt = domain.updatedAt!!
        )
    }
}


