package com.kdongsu5509.friends.controller.dto

import com.kdongsu5509.friends.service.dto.FriendRequestView
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
        fun from(view: FriendRequestView) = FriendRequestResponse(
            id = view.id!!,
            requester = FriendRequestUserResponse.from(view.requester),
            receiver = FriendRequestUserResponse.from(view.receiver),
            message = view.message,
            createdAt = view.createdAt!!,
            updatedAt = view.updatedAt!!
        )
    }
}
