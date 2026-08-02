package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import java.time.LocalDateTime
import java.util.*

data class FriendRestrictionView(
    val id: UUID?,
    val restrictor: FriendMember,
    val restricted: FriendMember,
    val type: FriendRelationStatus,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val expiredAt: LocalDateTime?
) {
    companion object {
        fun of(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendRestrictionView =
            FriendRestrictionView(
                id = relation.id,
                restrictor = members.getValue(relation.initiator()),
                restricted = members.getValue(relation.target()),
                type = relation.status,
                createdAt = relation.createdAt,
                updatedAt = relation.updatedAt,
                expiredAt = relation.rejectionExpiredAt
            )
    }
}
