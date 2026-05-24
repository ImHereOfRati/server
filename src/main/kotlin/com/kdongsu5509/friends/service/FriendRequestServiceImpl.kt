package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.controller.dto.FriendRequestViewType
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.friends.repository.mapper.FriendRequestMapper
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.port.FriendAlertPort
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendRequestServiceImpl(
    private val userService: UserService,
    private val friendRequestRepository: FriendRequestRepository,
    private val friendRestrictionRepository: FriendRestrictionRepository,
    private val friendshipRepository: FriendshipRepository,
    private val friendAlertPort: FriendAlertPort,
    private val friendRequestMapper: FriendRequestMapper
) : FriendRequestService {

    @Transactional
    override fun request(requesterEmail: String, receiverId: UUID, message: String): FriendRequest {
        val me: UserResult = userService.findByEmail(requesterEmail)
        val target = userService.findById(receiverId)

        verifyNotRestricted(requesterEmail, target.email)
        verifyNotAlreadyRequested(me, target)
        verifyNotAlreadyFriend(me, target)

        val friendRequest = FriendRequest.createWithNullId(me.toDomain(), target.toDomain(), message)
        val result = friendRequestRepository.save(friendRequest)

        return result
    }

    private fun verifyNotAlreadyFriend(me: UserResult, target: UserResult) {
        if (friendshipRepository.existsByOwnerUserIdAndFriendUserId(me.id, target.id))
            FriendException.ALREADY_FRIEND.throwIt()
    }

    private fun verifyNotAlreadyRequested(me: UserResult, target: UserResult) {
        if (friendRequestRepository.existsByRequesterIdAndReceiverId(me.id, target.id))
            FriendException.FRIEND_REQUEST_ALREADY_SENT.throwIt()
    }

    private fun verifyNotRestricted(requesterEmail: String, targetEmail: String) {
        if (friendRestrictionRepository.existsRestriction(requesterEmail, targetEmail)) {
            FriendException.FRIEND_REQUEST_UNPROCESSABLE_BY_ME.throwIt()
        }

        if (friendRestrictionRepository.existsRestriction(targetEmail, requesterEmail)) {
            FriendException.FRIEND_REQUEST_UNPROCESSABLE_BY_TARGET.throwIt()
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    override fun findAll(pageable: Pageable): Slice<FriendRequest> = friendRequestRepository.findAll(pageable)

    override fun findAllByEmailAndType(
        email: String,
        type: FriendRequestViewType,
        pageable: Pageable
    ): Slice<FriendRequest> {
        return when (type) {
            FriendRequestViewType.SENT -> friendRequestRepository.findAllByRequesterEmail(email, pageable)
            FriendRequestViewType.RECEIVED -> friendRequestRepository.findAllByReceiverEmail(email, pageable)
        }
    }

    override fun findById(id: UUID): FriendRequest {
        return friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
    }

    override fun findByIdAndParticipantEmail(id: UUID, participantEmail: String): FriendRequest {
        val found = findById(id)
        if (found.requester.email != participantEmail && found.receiver.email != participantEmail) {
            FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()
        }

        return found
    }

    @Transactional
    override fun acceptRequest(email: String, id: UUID): Friendship {
        val friendRequest = verifyRequestReceiver(email, id)
        val requesterFriendship = Friendship(
            owner = friendRequest.requester,
            friend = friendRequest.receiver,
            friendAlias = friendRequest.receiver.nickname
        )
        val receiverFriendship = Friendship(
            owner = friendRequest.receiver,
            friend = friendRequest.requester,
            friendAlias = friendRequest.requester.nickname
        )

        friendshipRepository.save(requesterFriendship)
        val result = friendshipRepository.save(receiverFriendship)
        friendRequestRepository.deleteById(id)

        return result
    }

    @Transactional
    override fun rejectRequest(email: String, id: UUID): FriendRestriction {
        val friendRequest = verifyRequestReceiver(email, id)
        val result = friendRestrictionRepository.save(
            FriendRestriction(
                restrictor = friendRequest.receiver,
                restricted = friendRequest.requester,
                type = FriendRestrictionType.REJECT
            )
        )

        friendRequestRepository.deleteById(id)

        return result
    }

    private fun verifyRequestReceiver(email: String, id: UUID): FriendRequest {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (found.receiverEmail() != email) FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()

        return found
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    override fun deleteById(id: UUID) {
        friendRequestRepository.deleteById(id)
    }

    @Transactional
    override fun deleteByIdAndReceiverEmail(id: UUID, receiverEmail: String) {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (found.receiverEmail() != receiverEmail) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()

        friendRequestRepository.deleteById(id)
    }

    @Transactional
    override fun deleteByIdAndRequesterEmail(id: UUID, requesterEmail: String) {
        val found = friendRequestRepository.findById(id) ?: FriendException.FRIEND_REQUEST_NOT_FOUND.throwIt()
        if (found.requester.email != requesterEmail) FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()

        friendRequestRepository.deleteById(id)
    }
}
