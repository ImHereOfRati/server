package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.friends.FriendException.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH
import com.kdongsu5509.friends.FriendException.SELF_BLOCK
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime
import java.util.*

class FriendRelation(
    val id: UUID? = null,
    val pair: FriendPair,
    val status: FriendRelationStatus,
    val modifierId: UUID,
    val message: RequestMessage? = null,
    val lowAlias: FriendAlias? = null,
    val highAlias: FriendAlias? = null,
    val rejectionExpiredAt: LocalDateTime? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    companion object {
        val PERMANENT = LocalDateTime.of(9999, 12, 31, 23, 59, 59)!!
        const val REJECT_BLOCKED_MONTH = 1L

        val RESTRICTED_STATUSES: List<FriendRelationStatus> =
            listOf(FriendRelationStatus.REJECTED, FriendRelationStatus.BLOCKED)

        fun blockWithoutRelation(blocker: UUID, target: UUID): FriendRelation {
            validateSelfValidating(blocker, target)
            return FriendRelation(
                pair = FriendPair.of(blocker, target),
                status = FriendRelationStatus.BLOCKED,
                modifierId = blocker,
                rejectionExpiredAt = PERMANENT
            )
        }

        fun validateSelfValidating(requesterId: UUID, targetId: UUID) {
            if (requesterId == targetId) {
                SELF_BLOCK.throwIt()
            }
        }
    }

    init {
        if (!pair.contains(modifierId))
            FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
    }

    constructor(requester: UUID, receiver: UUID, message: String) : this(
        pair = FriendPair.of(requester, receiver),
        status = FriendRelationStatus.REQUESTED,
        modifierId = requester,
        message = RequestMessage(message),
    )

    fun accept(lowNickname: String, highNickname: String): FriendRelation {
        validateIsRequested()
        return replaced(
            status = FriendRelationStatus.ACCEPTED,
            message = null,
            lowAlias = FriendAlias(highNickname),
            highAlias = FriendAlias(lowNickname),
            expiresAt = PERMANENT
        )
    }

    fun reject(current: LocalDateTime): FriendRelation {
        validateIsRequested()
        return replaced(
            status = FriendRelationStatus.REJECTED,
            initiatedBy = pair.counterpartOf(modifierId),
            message = null,
            expiresAt = current.plusMonths(REJECT_BLOCKED_MONTH)
        )
    }

    fun block(requesterId: UUID): FriendRelation {
        val targetId = getCounterpart(requesterId)
        validateOwnership(requesterId)
        validateSelfValidating(requesterId, targetId)

        return replaced(
            status = FriendRelationStatus.BLOCKED,
            initiatedBy = requesterId,
            message = null,
            lowAlias = null,
            highAlias = null,
            expiresAt = PERMANENT
        )
    }

    fun cancel(requesterId: UUID): FriendRelation {
        if (!this.isInitiatedBy(requesterId)) {
            FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
        }

        return replaced(
            status = FriendRelationStatus.CANCEL
        )
    }

    fun validateUnblockable(blockerId: UUID) {
        validateBlocked()
        if (!this.isInitiatedBy(blockerId)) FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
    }

    fun rename(owner: UUID, alias: String): FriendRelation {
        validateOwnership(owner)
        validateAccepted()
        val renamed = FriendAlias(alias)
        return if (pair.isLow(owner)) replaced(lowAlias = renamed) else replaced(highAlias = renamed)
    }

    fun getCounterpart(userId: UUID): UUID = pair.counterpartOf(userId)

    fun getAlias(userId: UUID): FriendAlias? = if (pair.isLow(userId)) lowAlias else highAlias

    fun initiator(): UUID = pair.memberOf(modifierId)

    fun target(): UUID = pair.counterpartOf(modifierId)

    fun involves(userId: UUID): Boolean = pair.contains(userId)

    fun isInitiatedBy(userId: UUID): Boolean = modifierId == userId

    fun viewTypeFor(userId: UUID): FriendRequestViewType =
        if (isInitiatedBy(userId)) FriendRequestViewType.SENT
        else FriendRequestViewType.RECEIVED

    private fun validateIsRequested() {
        if (status != FriendRelationStatus.REQUESTED) FriendException.FRIEND_REQUEST_ALREADY_HANDLED.throwIt()
    }

    private fun validateAccepted() {
        if (status != FriendRelationStatus.ACCEPTED)
            FriendException.FRIENDSHIP_NOT_ACCEPTED.throwIt()
    }

    private fun validateBlocked() {
        if (status != FriendRelationStatus.BLOCKED) FriendException.FRIENDSHIP_UNBLOCKED.throwIt()
    }

    private fun validateOwnership(userId: UUID) {
        if (!pair.contains(userId)) FRIEND_RELATIONSHIP_OWNER_MISS_MATCH.throwIt()
    }

    private fun replaced(
        status: FriendRelationStatus = this.status,
        initiatedBy: UUID = this.modifierId,
        message: RequestMessage? = this.message,
        lowAlias: FriendAlias? = this.lowAlias,
        highAlias: FriendAlias? = this.highAlias,
        expiresAt: LocalDateTime? = this.rejectionExpiredAt
    ): FriendRelation = FriendRelation(
        id = id,
        pair = pair,
        status = status,
        modifierId = initiatedBy,
        message = message,
        lowAlias = lowAlias,
        highAlias = highAlias,
        rejectionExpiredAt = expiresAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FriendRelation) return false

        if (id != null && other.id != null) return id == other.id
        return pair == other.pair
    }

    override fun hashCode(): Int = id?.hashCode() ?: pair.hashCode()
}
