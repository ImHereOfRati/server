package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.friends.repository.jpa.QFriendRequestJpaEntity
import com.kdongsu5509.friends.repository.jpa.QFriendRestrictionJpaEntity
import com.kdongsu5509.friends.repository.jpa.QFriendshipJpaEntity
import com.kdongsu5509.user.domain.UserStatus
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SpringQueryDSLUserRepository(private val queryFactory: JPAQueryFactory) {

    private val user = QUserJpaEntity.Companion.userJpaEntity

    fun findUserByEmail(email: String): UserJpaEntity? =
        queryFactory.selectFrom(user)
            .where(emailEquals(email))
            .fetchOne()


    fun findActiveUserByEmail(email: String): UserJpaEntity? =
        queryFactory.selectFrom(user)
            .where(emailEquals(email), isActive())
            .fetchOne()

    fun findActiveUsersByEmails(vararg emails: String): List<UserJpaEntity> =
        queryFactory.selectFrom(user)
            .where(user.email.`in`(*emails), isActive())
            .fetch()

    fun findActiveUsersByEmailAndId(email: String, id: UUID): List<UserJpaEntity> =
        queryFactory.selectFrom(user)
            .where(emailEquals(email).or(idEquals(id)), isActive())
            .fetch()

    //----

    fun findAll(pageable: Pageable): Slice<UserJpaEntity> {
        val content = queryFactory.selectFrom(user)
            .where(isNotWithdrawn())
            .fetch()
        val hasNext = content.size > pageable.pageSize
        val sliceContent = if (hasNext) content.subList(0, pageable.pageSize) else content

        return SliceImpl(sliceContent, pageable, hasNext)
    }

    fun findAllActiveByEmailAndKeyword(
        userEmail: String,
        keyword: String,
        pageable: Pageable = PageRequest.of(0, 20)
    ): Slice<UserJpaEntity> {
        val currentUserId = findCurrentUserId(userEmail) ?: return SliceImpl(emptyList(), pageable, false)
        val excludedUserIds = fetchExcludedUserIds(currentUserId)

        val content = queryFactory.selectFrom(user)
            .where(
                nicknameEqualsOrEmailEquals(keyword),
                isActive(),
                user.id.ne(currentUserId),
                if (excludedUserIds.isNotEmpty()) user.id.notIn(excludedUserIds) else null
            )
            .orderBy(user.id.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong() + 1)
            .fetch()

        val hasNext = content.size > pageable.pageSize
        val sliceContent = if (hasNext) content.subList(0, pageable.pageSize) else content

        return SliceImpl(sliceContent, pageable, hasNext)
    }

    private fun findCurrentUserId(userEmail: String): UUID? {
        return queryFactory.select(user.id)
            .from(user)
            .where(emailEquals(userEmail))
            .fetchOne()
    }

    private fun fetchExcludedUserIds(currentUserId: UUID): Set<UUID> {
        val friendUserIds = findFriendUserIds(currentUserId)
        val requestUserIds = findRequestUserIds(currentUserId)
        val restrictionUserIds = findRestrictionUserIds(currentUserId)
        return (friendUserIds + requestUserIds + restrictionUserIds).filterNotNull().toSet()
    }

    private fun findFriendUserIds(currentUserId: UUID): List<UUID?> {
        val friendship = QFriendshipJpaEntity.friendshipJpaEntity
        return queryFactory.select(friendship.friendUser.id)
            .from(friendship)
            .where(friendship.ownerUser.id.eq(currentUserId))
            .fetch() +
                queryFactory.select(friendship.ownerUser.id)
                    .from(friendship)
                    .where(friendship.friendUser.id.eq(currentUserId))
                    .fetch()
    }

    private fun findRequestUserIds(currentUserId: UUID): List<UUID?> {
        val friendRequest = QFriendRequestJpaEntity.friendRequestJpaEntity
        return queryFactory.select(friendRequest.receiver.id)
            .from(friendRequest)
            .where(friendRequest.requester.id.eq(currentUserId))
            .fetch() +
                queryFactory.select(friendRequest.requester.id)
                    .from(friendRequest)
                    .where(friendRequest.receiver.id.eq(currentUserId))
                    .fetch()
    }

    private fun findRestrictionUserIds(currentUserId: UUID): List<UUID?> {
        val friendRestriction = QFriendRestrictionJpaEntity.friendRestrictionJpaEntity
        return queryFactory.select(friendRestriction.restricted.id)
            .from(friendRestriction)
            .where(friendRestriction.restrictor.id.eq(currentUserId))
            .fetch() +
                queryFactory.select(friendRestriction.restrictor.id)
                    .from(friendRestriction)
                    .where(friendRestriction.restricted.id.eq(currentUserId))
                    .fetch()
    }

    private fun idEquals(id: UUID): BooleanExpression = user.id.eq(id)
    private fun emailEquals(email: String): BooleanExpression = user.email.eq(email)
    private fun isActive(): BooleanExpression = user.status.eq(UserStatus.ACTIVE)
    private fun isNotWithdrawn(): BooleanExpression = user.status.ne(UserStatus.WITHDRAWN)

    private fun nicknameEqualsOrEmailEquals(keyword: String): BooleanExpression =
        if (keyword.contains("@")) {
            emailEquals(keyword)
        } else {
            user.nickname.eq(keyword)
        }
}
