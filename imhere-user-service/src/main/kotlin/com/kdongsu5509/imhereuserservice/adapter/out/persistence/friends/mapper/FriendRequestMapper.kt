package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.mapper

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequest
import com.kdongsu5509.imhereuserservice.domain.friend.FriendRequestUserInfo
import org.springframework.stereotype.Component

@Component
class FriendRequestMapper {

    fun mapToDomainEntity(entity: FriendRequestJpaEntity): FriendRequest {
        val requester = entity.requester
        val receiver = entity.receiver

        val requestUserInfo = FriendRequestUserInfo(requester.id!!, requester.email, requester.nickname)
        val receiverUserInfo = FriendRequestUserInfo(receiver.id!!, receiver.email, receiver.nickname)

        return FriendRequest(
            entity.id,
            requestUserInfo,
            receiverUserInfo,
            entity.message,
            entity.createdAt
        )
    }
}