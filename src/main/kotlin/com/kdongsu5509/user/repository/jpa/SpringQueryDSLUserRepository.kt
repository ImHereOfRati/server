package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.friends.repository.jpa.QFriendRelationJpaEntity
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

    private val user = QUserJpaEntity.userJpaEntity

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

    /**
     * 이미 관계가 있는 사용자를 검색 결과에서 뺀다.
     *
     * 친구/요청/제한이 세 테이블로 나뉘어 있을 때는 각각을 양방향으로 뒤져 여섯 번 조회했다.
     * 관계가 한 테이블 한 행이 된 뒤로는 내가 낀 행에서 상대 식별자만 꺼내면 되므로 두 번이면 된다.
     */
    private fun fetchExcludedUserIds(currentUserId: UUID): Set<UUID> {
        val relation = QFriendRelationJpaEntity.friendRelationJpaEntity

        val asLow = queryFactory.select(relation.highUserId)
            .from(relation)
            .where(relation.lowUserId.eq(currentUserId))
            .fetch()

        val asHigh = queryFactory.select(relation.lowUserId)
            .from(relation)
            .where(relation.highUserId.eq(currentUserId))
            .fetch()

        return (asLow + asHigh).filterNotNull().toSet()
    }

    private fun emailEquals(email: String): BooleanExpression = user.email.eq(email)
    private fun isActive(): BooleanExpression = user.status.eq(UserStatus.ACTIVE)

    private fun nicknameEqualsOrEmailEquals(keyword: String): BooleanExpression =
        if (keyword.contains("@")) {
            emailEquals(keyword)
        } else {
            user.nickname.eq(keyword)
        }
}
