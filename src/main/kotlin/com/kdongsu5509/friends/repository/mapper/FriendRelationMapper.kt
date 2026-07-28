package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.friends.domain.FriendAlias
import com.kdongsu5509.friends.domain.FriendPair
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.RequestMessage
import com.kdongsu5509.friends.repository.jpa.FriendRelationJpaEntity
import org.springframework.stereotype.Component

@Component
class FriendRelationMapper {

    fun toDomain(entity: FriendRelationJpaEntity): FriendRelation = FriendRelation(
        id = entity.id!!,
        pair = FriendPair.restore(entity.lowUserId, entity.highUserId),
        status = entity.status,
        modifierId = entity.initiatedUserId,
        message = entity.message?.let(::RequestMessage),
        lowAlias = entity.lowAlias?.let(::FriendAlias),
        highAlias = entity.highAlias?.let(::FriendAlias),
        rejectionExpiredAt = entity.expiredAt,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun toEntity(relation: FriendRelation): FriendRelationJpaEntity = FriendRelationJpaEntity(
        lowUserId = relation.pair.low,
        highUserId = relation.pair.high,
        status = relation.status,
        initiatedUserId = relation.modifierId,
        message = relation.message?.value,
        lowAlias = relation.lowAlias?.value,
        highAlias = relation.highAlias?.value,
        expiredAt = relation.rejectionExpiredAt
    )

    fun applyTo(entity: FriendRelationJpaEntity, relation: FriendRelation) {
        entity.apply(
            status = relation.status,
            initiatedBy = relation.modifierId,
            message = relation.message?.value,
            lowAlias = relation.lowAlias?.value,
            highAlias = relation.highAlias?.value,
            expiredAt = relation.rejectionExpiredAt
        )
    }
}
