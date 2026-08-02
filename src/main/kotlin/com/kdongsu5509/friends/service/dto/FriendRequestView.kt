package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import java.time.LocalDateTime
import java.util.*

data class FriendRequestView(
    val id: UUID?,
    val requester: FriendMember,
    val receiver: FriendMember,
    val message: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendRequestView =
            FriendRequestView(
                id = relation.id,
                requester = members.getValue(relation.initiator()),
                receiver = members.getValue(relation.target()),
                message = relation.message?.value.orEmpty(),
                createdAt = relation.createdAt,
                updatedAt = relation.updatedAt
            )
    }
}
