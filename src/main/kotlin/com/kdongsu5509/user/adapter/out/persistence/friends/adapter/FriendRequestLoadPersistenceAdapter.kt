package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRequestMapper
import com.kdongsu5509.user.application.port.out.friend.FriendRequestLoadPort
import com.kdongsu5509.user.domain.friend.FriendRequest
import org.springframework.stereotype.Component

@Component
class FriendRequestLoadPersistenceAdapter(
    private val friendRequestMapper: FriendRequestMapper,
    private val springDataFriendRequestRepository: SpringDataFriendRequestRepository,
) : FriendRequestLoadPort {
    override fun findReceivedRequestsAllByEmail(email: String): List<FriendRequest> {
        val queryResults = springDataFriendRequestRepository.findByReceiverEmail(email)

        if (queryResults.isEmpty()) return listOf()

        return queryResults.map { entity ->
            friendRequestMapper.mapToDomainEntity(entity)
        }
    }

    override fun findReceivedRequestByRequestId(requestId: Long): FriendRequest {
        val queryResult = springDataFriendRequestRepository.findById(requestId).orElseThrow {
            throw BusinessException(FriendErrorCode.FRIENDSHIP_REQUEST_NOT_FOUND)
        }

        return friendRequestMapper.mapToDomainEntity(queryResult)
    }
}