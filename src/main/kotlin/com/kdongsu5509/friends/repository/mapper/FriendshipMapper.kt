package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.jpa.FriendshipJpaEntity
import com.kdongsu5509.user.repository.UserMapper
import org.springframework.stereotype.Component

@Component
class FriendshipMapper(
    private val userMapper: UserMapper
) {
    fun toDomain(entity: FriendshipJpaEntity) =
        Friendship(
            id = entity.id,
            owner = userMapper.toDomain(entity.ownerUser)!!,
            friend = userMapper.toDomain(entity.friendUser)!!,
            friendAlias = entity.friendAlias,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    fun toEntity(domain: Friendship) =
        FriendshipJpaEntity(
            ownerUser = userMapper.toEntity(domain.owner),
            friendUser = userMapper.toEntity(domain.friend),
            friendAlias = domain.friendAlias
        )
}
