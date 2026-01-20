package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friend

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.FriendshipJpaEntity
import com.kdongsu5509.imhereuserservice.domain.Friend
import org.springframework.stereotype.Component

@Component
class FriendMapper {
    fun mapToDomainEntity(entity: FriendshipJpaEntity, currentUserEmail: String): Friend {
        val isRequester = entity.requester!!.email == currentUserEmail
        val opponent = if (isRequester) entity.receiver!! else entity.requester!!

        return Friend(
            friendshipId = entity.id!!,
            opponentId = opponent.id!!,
            opponentEmail = opponent.email,
            opponentNickname = opponent.nickname,
            status = entity.friendshipStatus!!
        )
    }
}