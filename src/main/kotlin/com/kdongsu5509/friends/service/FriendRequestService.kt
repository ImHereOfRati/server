package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRequestViewType
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.shared.notification.NotificationPort
import com.kdongsu5509.shared.notification.dto.NotificationCategory
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import com.kdongsu5509.shared.notification.dto.NotificationSendRequest
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendRequestService(
    private val userLookupContract: UserLookupContract,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendRestrictionRepository: FriendRestrictionRepository,
    private val friendshipRepository: FriendshipRepository,
    private val notificationPort: NotificationPort,
) {

    private val friendRequestPolicy =
        FriendRequestPolicy(friendRequestRepository, friendRestrictionRepository, friendshipRepository)

    @Transactional
    fun request(requesterEmail: String, receiverId: UUID, message: String): FriendRequest {
        val me: UserResult = userLookupContract.findByEmail(requesterEmail)
        val target = userLookupContract.findById(receiverId)

        friendRequestPolicy.verifyRequestable(me, target)

        val friendRequest = FriendRequest.newRequest(me.toDomain(), target.toDomain(), message)
        val result = friendRequestRepository.save(friendRequest)

        notificationPort.send(
            NotificationSendRequest(
                category = NotificationCategory.FRIEND_REQUEST_RECEIVED,
                sender = NotificationPersonInfo(me.email, me.nickname),
                receiver = NotificationPersonInfo(target.email, target.nickname)
            )
        )

        return result
    }

    fun findAllByEmailAndType(
        email: String,
        type: FriendRequestViewType,
        pageable: Pageable
    ): Slice<FriendRequest> {
        return when (type) {
            FriendRequestViewType.SENT -> friendRequestRepository.findAllByRequesterEmail(email, pageable)
            FriendRequestViewType.RECEIVED -> friendRequestRepository.findAllByReceiverEmail(email, pageable)
        }
    }

    fun findById(id: UUID): FriendRequest {
        return friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
    }

    fun findByIdAndParticipantEmail(id: UUID, participantEmail: String): FriendRequest {
        val found = findById(id)
        if (!found.involves(participantEmail)) {
            FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()
        }

        return found
    }

    @Transactional
    fun acceptRequest(email: String, id: UUID): Friendship {
        val friendRequest = verifyRequestReceiver(email, id)
        val (requesterFriendship, receiverFriendship) = friendRequest.accept()

        friendshipRepository.save(requesterFriendship)
        val result = friendshipRepository.save(receiverFriendship)
        friendRequestRepository.deleteById(id)

        notificationPort.send(
            NotificationSendRequest(
                category = NotificationCategory.FRIEND_REQUEST_ACCEPTED,
                sender = NotificationPersonInfo(friendRequest.receiver.email, friendRequest.receiver.nickname),
                receiver = NotificationPersonInfo(friendRequest.requester.email, friendRequest.requester.nickname)
            )
        )

        return result
    }

    @Transactional
    fun rejectRequest(email: String, id: UUID): FriendRestriction {
        val friendRequest = verifyRequestReceiver(email, id)
        val result = friendRestrictionRepository.save(friendRequest.reject())

        friendRequestRepository.deleteById(id)

        return result
    }

    private fun verifyRequestReceiver(email: String, id: UUID): FriendRequest {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (!found.isReceivedBy(email)) FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()

        return found
    }

    @Transactional
    fun deleteByIdAndReceiverEmail(id: UUID, receiverEmail: String) {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (!found.isReceivedBy(receiverEmail)) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()

        friendRequestRepository.deleteById(id)
    }

    @Transactional
    fun deleteByIdAndRequesterEmail(id: UUID, requesterEmail: String) {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (!found.isRequestedBy(requesterEmail)) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()

        friendRequestRepository.deleteById(id)
    }
}
