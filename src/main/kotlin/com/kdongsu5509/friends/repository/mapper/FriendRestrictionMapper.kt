package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.repository.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.user.repository.UserMapper
import org.springframework.stereotype.Component

@Component
class FriendRestrictionMapper(
    private val userMapper: UserMapper
) {

    fun toDomain(entity: FriendRestrictionJpaEntity) = FriendRestriction(
        id = entity.id,
        restrictor = userMapper.toDomain(entity.restrictor)!!,
        restricted = userMapper.toDomain(entity.restricted)!!,
        type = entity.type,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        expiredAt = entity.expiredAt
    )

    fun toEntity(domain: FriendRestriction) = FriendRestrictionJpaEntity.create(
        restrictor = userMapper.toEntity(domain.restrictor),
        restricted = userMapper.toEntity(domain.restricted),
        type = domain.type,
        expiredAt = domain.expiredAt
    )
}
