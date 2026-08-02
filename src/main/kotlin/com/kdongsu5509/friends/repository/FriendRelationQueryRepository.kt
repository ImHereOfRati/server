package com.kdongsu5509.friends.repository

import com.kdongsu5509.friends.domain.FriendPair
import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.domain.FriendRequestViewType
import com.kdongsu5509.friends.repository.jpa.FriendRelationQueryDslRepository
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRelationRepository
import com.kdongsu5509.friends.repository.mapper.FriendRelationMapper
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class FriendRelationQueryRepository(
    private val springDataFriendRelationRepository: SpringDataFriendRelationRepository,
    private val friendRelationQueryDslRepository: FriendRelationQueryDslRepository,
    private val friendRelationMapper: FriendRelationMapper
) {
    fun findRequests(userId: UUID, type: FriendRequestViewType, pageable: Pageable): Slice<FriendRelation> =
        when (type) {
            FriendRequestViewType.SENT ->
                friendRelationQueryDslRepository.findAllInitiatedBy(
                    userId,
                    FriendRelationStatus.REQUESTED,
                    pageable
                )

            FriendRequestViewType.RECEIVED ->
                friendRelationQueryDslRepository.findAllTargetedAt(
                    userId,
                    FriendRelationStatus.REQUESTED,
                    pageable
                )
        }.map(friendRelationMapper::toDomain)

    fun findRequest(id: UUID): FriendRelation? =
        springDataFriendRelationRepository
            .findByIdAndStatus(id, FriendRelationStatus.REQUESTED)
            ?.let(friendRelationMapper::toDomain)

    // --- 친구 -------------------------------------------------------------

    fun findFriendships(ownerId: UUID, pageable: Pageable): Slice<FriendRelation> =
        friendRelationQueryDslRepository
            .findAllByParticipantAndStatus(ownerId, FriendRelationStatus.ACCEPTED, pageable)
            .map(friendRelationMapper::toDomain)

    fun findFriendshipByPair(ownerId: UUID, targetId: UUID): FriendRelation? {
        val (low, high) = FriendPair.ordered(ownerId, targetId)
        return springDataFriendRelationRepository
            .findByLowUserIdAndHighUserIdAndStatus(low, high, FriendRelationStatus.ACCEPTED)
            ?.let(friendRelationMapper::toDomain)
    }

    fun findFriendshipById(id: UUID): FriendRelation? =
        springDataFriendRelationRepository
            .findByIdAndStatus(id, FriendRelationStatus.ACCEPTED)
            ?.let(friendRelationMapper::toDomain)

    // --- 제한 -------------------------------------------------------------

    fun findRestrictions(restrictorId: UUID, pageable: Pageable): Slice<FriendRelation> =
        friendRelationQueryDslRepository
            .findAllInitiatedByWithStatuses(restrictorId, FriendRelation.RESTRICTED_STATUSES, pageable)
            .map(friendRelationMapper::toDomain)
    
    fun existsActiveRestriction(
        restrictorId: UUID,
        targetId: UUID,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val (low, high) = FriendPair.ordered(restrictorId, targetId)
        return friendRelationQueryDslRepository.existsActiveRestriction(
            lowUserId = low,
            highUserId = high,
            restrictorId = restrictorId,
            statuses = FriendRelation.RESTRICTED_STATUSES,
            now = now
        )
    }

    // --- 사용자 검색 ------------------------------------------------------

    fun findRelatedUserIds(userId: UUID): Set<UUID> =
        friendRelationQueryDslRepository.findRelatedUserIds(userId)

    // --- 관리자 -----------------------------------------------------------

    fun findAllRequests(pageable: Pageable): Slice<FriendRelation> =
        springDataFriendRelationRepository
            .findAllByStatus(FriendRelationStatus.REQUESTED, pageable)
            .map(friendRelationMapper::toDomain)

    fun findAllFriendships(pageable: Pageable): Slice<FriendRelation> =
        springDataFriendRelationRepository
            .findAllByStatus(FriendRelationStatus.ACCEPTED, pageable)
            .map(friendRelationMapper::toDomain)

    fun findAllRestrictions(pageable: Pageable): Slice<FriendRelation> =
        springDataFriendRelationRepository
            .findAllByStatusIn(FriendRelation.RESTRICTED_STATUSES, pageable)
            .map(friendRelationMapper::toDomain)
}
