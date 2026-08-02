package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.FriendException.FRIEND_RELATIONSHIP_NOT_FOUND
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.friends.repository.FriendRelationRepository
import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.friends.service.dto.FriendRequestView
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.friends.service.dto.FriendshipView
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserLookupContract
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class FriendRelationCommandService(
    private val friendRelationRepository: FriendRelationRepository,
    private val friendMemberLoader: FriendMemberLoader,
    private val userLookupContract: UserLookupContract,
    private val eventPublisher: DomainEventPublisher,
) {
    fun sendRequest(requesterId: UUID, receiverId: UUID, message: String): FriendRequestView {
        val requester = FriendMember.from(userLookupContract.findById(requesterId))
        val receiver = FriendMember.from(userLookupContract.findById(receiverId))

        val existingRelation = friendRelationRepository.findByPair(requester.id, receiver.id)
        if (existingRelation != null) throwExplicitUnRequestableReason(existingRelation)

        val saved = friendRelationRepository.save(
            FriendRelation(
                requester.id,
                receiver.id,
                message
            )
        )

        eventPublisher.publish(
            FriendRequestSent(
                requesterId = requester.id,
                requesterNickname = requester.nickname,
                receiverId = receiver.id,
            )
        )

        return FriendRequestView.of(saved, membersOf(requester, receiver))
    }

    fun acceptRequest(requestId: UUID, accepterId: UUID): FriendshipView {
        val relation = findReceivedFriendRequests(requestId, accepterId)
        val members = friendMemberLoader.of(relation)

        val accepted = friendRelationRepository.save(
            relation.accept(
                lowNickname = members.getValue(relation.pair.low).nickname,
                highNickname = members.getValue(relation.pair.high).nickname
            )
        )

        val initiator = members.getValue(accepted.initiator())
        val target = members.getValue(accepted.target())

        eventPublisher.publish(
            FriendRequestAccepted(
                accepterId = target.id,
                accepterNickname = target.nickname,
                requesterId = initiator.id,
            )
        )

        return FriendshipView.of(accepted, accepterId, members)
    }

    fun rejectRequest(relationId: UUID, rejecterId: UUID): FriendRestrictionView {
        val relation = findReceivedFriendRequests(relationId, rejecterId)
        val rejected = friendRelationRepository.save(relation.reject(LocalDateTime.now()))

        return FriendRestrictionView.of(
            rejected,
            friendMemberLoader.of(rejected)
        )
    }

    fun deleteReceivedRequest(requestId: UUID, receiverId: UUID) {
        findReceivedFriendRequests(requestId, receiverId)
        friendRelationRepository.deleteById(requestId)
    }

    fun cancelSentRequest(requestId: UUID, requesterId: UUID) {
        val relation = findSpecificStatusRelation(requestId, FriendRelationStatus.REQUESTED)
        val canceled = relation.cancel(requesterId)
        friendRelationRepository.deleteById(canceled.id!!)
    }

    // --- 친구 -------------------------------------------------------------

    fun updateAlias(friendshipId: UUID, requesterId: UUID, alias: String): FriendshipView {
        val renamed = friendRelationRepository.save(
            findFriendship(friendshipId, requesterId).rename(requesterId, alias)
        )
        return FriendshipView.of(renamed, requesterId, friendMemberLoader.of(renamed))
    }

    fun deleteFriendship(friendshipId: UUID, ownerId: UUID) {
        findFriendship(friendshipId, ownerId)
        friendRelationRepository.deleteById(friendshipId)
    }

    fun block(blockerId: UUID, targetUserId: UUID): FriendRestrictionView {
        val blocker = FriendMember.from(userLookupContract.findById(blockerId))

        val target = try {
            FriendMember.from(userLookupContract.findById(targetUserId))
        } catch (e: ImHereBaseException) {
            FriendException.BLOCK_TARGET_NOT_FOUND.throwIt(
                contextData = mapOf("targetUserId" to targetUserId),
                cause = e
            )
        }

        val blocked = friendRelationRepository.findByPair(blocker.id, target.id)
            ?.block(blocker.id)
            ?: FriendRelation.blockWithoutRelation(blocker.id, target.id)

        return FriendRestrictionView.of(
            friendRelationRepository.save(blocked),
            membersOf(blocker, target)
        )
    }

    fun unblock(blockerId: UUID, targetUserId: UUID) {
        val relation = friendRelationRepository.findByPair(blockerId, targetUserId) ?: return
        relation.validateUnblockable(blockerId)
        friendRelationRepository.deleteById(relation.id!!)
    }

    private fun throwExplicitUnRequestableReason(relation: FriendRelation) {
        when (relation.status) {
            FriendRelationStatus.ACCEPTED -> FriendException.ALREADY_FRIEND.throwIt()
            FriendRelationStatus.REQUESTED -> FriendException.FRIEND_REQUEST_ALREADY_SENT.throwIt()
            FriendRelationStatus.REJECTED, FriendRelationStatus.BLOCKED ->
                FriendException.FRIEND_REQUEST_UNPROCESSABLE.throwIt()

            else -> {
                CommonErrorCode.INVALID_INPUT.throwIt()
            }
        }
    }

    private fun membersOf(vararg members: FriendMember): Map<UUID, FriendMember> =
        members.associateBy { it.id }

    private fun findReceivedFriendRequests(id: UUID, requesterId: UUID): FriendRelation {
        val relation = findSpecificStatusRelation(id, FriendRelationStatus.REQUESTED)
        if (relation.isInitiatedBy(requesterId) || !relation.involves(requesterId)) {
            FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()
        }
        return relation
    }

    private fun findFriendship(id: UUID, requesterId: UUID): FriendRelation {
        val relation = findSpecificStatusRelation(id, FriendRelationStatus.ACCEPTED)
        if (!relation.involves(requesterId)) {
            FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        }
        return relation
    }

    private fun findSpecificStatusRelation(id: UUID, expected: FriendRelationStatus): FriendRelation {
        val relation = friendRelationRepository.findById(id)
            ?: FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        if (relation.status != expected) FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        return relation
    }
}
