package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.repository.UserMapper
import org.springframework.stereotype.Component

@Component
class FriendRequestMapper(
    private val userMapper: UserMapper
) {
    fun toDomain(entity: FriendRequestJpaEntity) =
        FriendRequest(
            id = entity.id,
            requester = userMapper.toDomain(entity.requester)!!,
            receiver = userMapper.toDomain(entity.receiver)!!,
            message = entity.message,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )

    fun toEntity(domain: FriendRequest) =
        FriendRequestJpaEntity(
            requester = userMapper.toEntity(domain.requester),
            receiver = userMapper.toEntity(domain.receiver),
            message = domain.message
        )
}
