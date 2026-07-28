package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.domain.FriendRequestViewType
import com.kdongsu5509.friends.repository.FriendRelationQueryRepository
import com.kdongsu5509.friends.service.dto.FriendRequestView
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.friends.service.dto.FriendshipView
import com.kdongsu5509.support.exception.throwIt
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class FriendRelationQueryService(
    private val friendRelationQueryRepository: FriendRelationQueryRepository,
    private val friendMemberLoader: FriendMemberLoader,
) {
    fun findRequests(
        ownerId: UUID,
        type: FriendRequestViewType,
        pageable: Pageable
    ): Slice<FriendRequestView> {
        val relations = friendRelationQueryRepository.findRequests(ownerId, type, pageable)
        return friendMemberLoader.toViews(relations, FriendRequestView::of)
    }

    fun findRequest(requestId: UUID, requesterId: UUID): FriendRequestView {
        val relation = friendRelationQueryRepository.findRequest(requestId)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        if (!relation.involves(requesterId)) {
            FriendException.FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH.throwIt()
        }

        return FriendRequestView.of(relation, friendMemberLoader.of(relation))
    }

    fun findFriends(requesterIdId: UUID, pageable: Pageable): Slice<FriendshipView> {
        val friendships = friendRelationQueryRepository.findFriendships(requesterIdId, pageable)
        return friendMemberLoader.toViews(friendships) { relation, members ->
            FriendshipView.of(
                relation,
                requesterIdId,
                members
            )
        }
    }

    fun findFriendByTarget(requesterId: UUID, targetUserId: UUID): FriendshipView? {
        if (requesterId == targetUserId) return null

        val relation = friendRelationQueryRepository.findFriendshipByPair(requesterId, targetUserId) ?: return null
        return FriendshipView.of(relation, requesterId, friendMemberLoader.of(relation))
    }

    fun findFriend(friendshipId: UUID, requesterId: UUID): FriendshipView {
        val relation = friendRelationQueryRepository.findFriendshipById(friendshipId)
            ?: FriendException.FRIEND_RELATIONSHIP_NOT_FOUND.throwIt()

        if (!relation.involves(requesterId)) {
            FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        }
        return FriendshipView.of(relation, requesterId, friendMemberLoader.of(relation))
    }

    fun findRestrictions(requesterId: UUID, pageable: Pageable): Slice<FriendRestrictionView> =
        friendMemberLoader.toViews(
            friendRelationQueryRepository.findRestrictions(requesterId, pageable),
            FriendRestrictionView::of
        )

    fun existsRestriction(requesterId: UUID, targetUserId: UUID): Boolean {
        if (requesterId == targetUserId) return false
        return friendRelationQueryRepository.existsActiveRestriction(requesterId, targetUserId)
    }
}
