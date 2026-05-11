package com.kdongsu5509.user.adapter.out.persistence.user.jpa

import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.QFriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.QFriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.QFriendRestrictionJpaEntity
import com.kdongsu5509.user.domain.user.UserStatus
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SpringQueryDSLUserRepository(private val queryFactory: JPAQueryFactory) {

    private val user = QUserJpaEntity.userJpaEntity

    fun findUserByEmail(email: String): UserJpaEntity? =
        queryFactory.selectFrom(user)
            .where(emailEq(email))
            .fetchOne()

    fun findActiveUserByEmail(email: String): UserJpaEntity? =
        queryFactory.selectFrom(user)
            .where(emailEq(email), isActive())
            .fetchOne()

    fun findActiveUsersByEmails(vararg emails: String): List<UserJpaEntity> =
        queryFactory.selectFrom(user)
            .where(user.email.`in`(*emails), isActive())
            .fetch()

    fun findActiveUsersByEmailAndId(email: String, id: UUID): List<UserJpaEntity> =
        queryFactory.selectFrom(user)
            .where(emailEq(email).or(idEq(id)), isActive())
            .fetch()

    fun searchNewFriendCandidates(userEmail: String, keyword: String): List<UserJpaEntity> {
        val currentUserId = queryFactory.select(user.id)
            .from(user)
            .where(emailEq(userEmail))
            .fetchOne() ?: return emptyList()

        return queryFactory.selectFrom(user)
            .where(
                nicknameEqOrEmailEq(keyword),
                isActive(),
                user.id.ne(currentUserId),
                isNotRelated(currentUserId)
            ).fetch()
    }

    private fun idEq(id: UUID): BooleanExpression = user.id.eq(id)
    private fun emailEq(email: String): BooleanExpression = user.email.eq(email)
    private fun isActive(): BooleanExpression = user.status.eq(UserStatus.ACTIVE)

    private fun nicknameEqOrEmailEq(keyword: String): BooleanExpression =
        user.nickname.eq(keyword).or(emailEq(keyword))

    private fun isNotRelated(currentUserId: UUID): BooleanExpression =
        isNotFriend(currentUserId)
            .and(isNotRequested(currentUserId))
            .and(isNotRestricted(currentUserId))

    private fun isNotFriend(currentUserId: UUID): BooleanExpression {
        val rel = QFriendRelationshipsJpaEntity.friendRelationshipsJpaEntity
        return JPAExpressions.selectOne()
            .from(rel)
            .where(
                (rel.ownerUser.id.eq(currentUserId).and(rel.friendUser.id.eq(user.id)))
                    .or(rel.ownerUser.id.eq(user.id).and(rel.friendUser.id.eq(currentUserId)))
            ).notExists()
    }

    private fun isNotRequested(currentUserId: UUID): BooleanExpression {
        val req = QFriendRequestJpaEntity.friendRequestJpaEntity
        return JPAExpressions.selectOne()
            .from(req)
            .where(
                (req.requester.id.eq(currentUserId).and(req.receiver.id.eq(user.id)))
                    .or(req.receiver.id.eq(currentUserId).and(req.requester.id.eq(user.id)))
            ).notExists()
    }

    private fun isNotRestricted(currentUserId: UUID): BooleanExpression {
        val res = QFriendRestrictionJpaEntity.friendRestrictionJpaEntity
        return JPAExpressions.selectOne()
            .from(res)
            .where(
                (res.actor.id.eq(currentUserId).and(res.target.id.eq(user.id)))
                    .or(res.target.id.eq(currentUserId).and(res.actor.id.eq(user.id)))
            ).notExists()
    }
}
