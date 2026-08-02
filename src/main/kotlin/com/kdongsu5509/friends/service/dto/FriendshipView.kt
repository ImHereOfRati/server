package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelation
import java.time.LocalDateTime
import java.util.*

data class FriendshipView(
    val id: UUID?,
    val owner: FriendMember,
    val friend: FriendMember,
    val friendAlias: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(
            relation: FriendRelation,
            viewerId: UUID,
            members: Map<UUID, FriendMember>
        ): FriendshipView = FriendshipView(
            id = relation.id,
            owner = members.getValue(relation.pair.memberOf(viewerId)),
            friend = members.getValue(relation.getCounterpart(viewerId)),
            friendAlias = relation.getAlias(viewerId)?.value.orEmpty(),
            createdAt = relation.createdAt,
            updatedAt = relation.updatedAt
        )

        fun ofAny(relation: FriendRelation, members: Map<UUID, FriendMember>): FriendshipView =
            of(relation, relation.pair.low, members)
    }
}
