package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import com.kdongsu5509.user.application.port.`in`.friend.UpdateFriendRequestUseCase
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipSavePort
import com.kdongsu5509.user.application.port.out.friend.FriendRequestLoadPort
import com.kdongsu5509.user.application.port.out.friend.FriendRequestUpdatePort
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionSavePort
import com.kdongsu5509.user.domain.friend.FriendRelationship
import com.kdongsu5509.user.domain.friend.FriendRequest
import com.kdongsu5509.user.domain.friend.FriendRestriction
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class FriendRequestUpdateService(
    private val friendRequestLoadPort: FriendRequestLoadPort,
    private val friendRequestUpdatePort: FriendRequestUpdatePort,
    private val friendRelationshipSavePort: FriendRelationshipSavePort,
    private val friendRestrictionSavePort: FriendRestrictionSavePort
) : UpdateFriendRequestUseCase {

    override fun acceptFriendRequest(userEmail: String, friendRequestId: Long): FriendRelationship {
        val friendRequestQueryResult = verifyAcceptRequest(friendRequestId, userEmail)

        val acceptanceResult = friendRelationshipSavePort.save(
            requester = friendRequestQueryResult.requester,
            receiver = friendRequestQueryResult.receiver
        )

        friendRequestUpdatePort.delete(friendRequestQueryResult.friendRequestId!!)
        return acceptanceResult
    }

    override fun rejectFriendRequest(
        userEmail: String,
        friendRequestId: Long
    ): FriendRestriction {
        val friendRequestQueryResult = verifyAcceptRequest(friendRequestId, userEmail)

        val rejectionResult = friendRestrictionSavePort.save(
            requester = friendRequestQueryResult.requester,
            receiver = friendRequestQueryResult.receiver,
            type = FriendRestrictionType.REJECT
        )

        friendRequestUpdatePort.delete(friendRequestQueryResult.friendRequestId!!)

        return rejectionResult
    }

    private fun verifyAcceptRequest(
        friendRequestId: Long, userEmail: String
    ): FriendRequest {
        val friendRequestQueryResult = friendRequestLoadPort.findReceivedRequestByRequestId(friendRequestId)

        if (friendRequestQueryResult.receiver.email != userEmail) {
            throw BusinessException(FriendErrorCode.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH)
        }

        return friendRequestQueryResult
    }
}