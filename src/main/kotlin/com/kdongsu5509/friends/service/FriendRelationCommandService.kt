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

/**
 * 관계를 바꾸는 유스케이스.
 *
 * 조회와 갈라 둔 이유는 두 쪽이 필요로 하는 것이 다르기 때문이다. 여기서는 상태 전이를 걸어야
 * 하므로 [FriendRelation]을 다뤄야 하고, 되돌리는 값이 곧 규칙을 지키는 근거가 된다.
 *
 * 애그리게이트는 식별자만 안다. 별칭에 쓸 닉네임이나 알림에 쓸 이메일처럼 사용자 표시 정보가
 * 필요한 자리에서만 [UserLookupContract]로 가져와 넘긴다. 삭제처럼 표시가 필요 없는 명령은
 * 사용자 조회를 일으키지 않는다.
 *
 * 클래스가 갈라지면서 트랜잭션 경계도 갈라진다. 이 클래스는 통째로 쓰기 트랜잭션이고,
 * 조회 쪽은 통째로 읽기 전용이다.
 */
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
                requesterEmail = requester.email,
                requesterNickname = requester.nickname,
                receiverEmail = receiver.email,
            )
        )

        return FriendRequestView.of(saved, membersOf(requester, receiver))
    }

    fun acceptRequest(requestId: UUID, accepterId: UUID): FriendshipView {
        val relation = findReceivedFriendRequests(requestId, accepterId)
        val members = friendMemberLoader.of(relation)

        val accepted = friendRelationRepository.save(
            relation.accept(
                lowAlias = members.getValue(relation.pair.low).nickname,
                highAlias = members.getValue(relation.pair.high).nickname
            )
        )

        val initiator = members.getValue(accepted.initiator())
        val target = members.getValue(accepted.target())

        eventPublisher.publish(
            FriendRequestAccepted(
                accepterEmail = target.email,
                accepterNickname = target.nickname,
                requesterEmail = initiator.email,
            )
        )

        return FriendshipView.of(accepted, accepterId, members)
    }

    /** 거절한 쪽이 제한의 주체가 되므로 관계의 방향이 뒤집힌다. */
    fun rejectRequest(relationId: UUID, rejecterId: UUID): FriendRestrictionView {
        val relation = findReceivedFriendRequests(relationId, rejecterId)
        val rejected = friendRelationRepository.save(relation.reject(LocalDateTime.now()))

        return FriendRestrictionView.of(
            rejected,
            friendMemberLoader.of(rejected)
        )
    }

    /** 받은 요청을 지운다. 거절과 달리 제한 기록을 남기지 않는다. */
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
