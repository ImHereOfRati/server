package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRequestMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRequestSavePort
import com.kdongsu5509.user.domain.friend.FriendRequest
import org.springframework.stereotype.Component
import java.util.*

@Component
class FriendRequestSavePersistenceAdapter(
    private val friendRequestMapper: FriendRequestMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRequestRepository: SpringDataFriendRequestRepository,
) : FriendRequestSavePort {
    override fun save(requesterEmail: String, receiverId: UUID, message: String): FriendRequest {
        val queryResult = fetchRequiredUsers(requesterEmail, receiverId)

        val requester = queryResult.first { it.email == requesterEmail }
        val receiver = queryResult.first { it.id == receiverId }

        val newFriendRequest = FriendRequestJpaEntity(
            requester = requester,
            receiver = receiver,
            message = message
        )

        val commandResult = springDataFriendRequestRepository.save(newFriendRequest)
        return friendRequestMapper.mapToDomainEntity(commandResult)
    }

    private fun fetchRequiredUsers(
        requesterEmail: String,
        receiverId: UUID
    ): List<UserJpaEntity> {
        val queryResult = userRepository.findActiveUsersByEmailAndId(requesterEmail, receiverId)

        if (queryResult.size != 2) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
        return queryResult
    }
}