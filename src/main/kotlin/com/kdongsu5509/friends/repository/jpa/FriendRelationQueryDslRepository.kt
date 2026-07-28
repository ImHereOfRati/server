package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.querydsl.core.types.Expression
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.PathBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
class FriendRelationQueryDslRepository(
    private val queryFactory: JPAQueryFactory
) {

    private val relation = QFriendRelationJpaEntity.friendRelationJpaEntity

    /** 상태가 같은, 내가 낀 모든 관계. 방향을 가리지 않는다. */
    fun findAllByParticipantAndStatus(
        userId: UUID,
        status: FriendRelationStatus,
        pageable: Pageable
    ): Slice<FriendRelationJpaEntity> =
        slice(pageable, participates(userId), statusEq(status))

    /** 내가 건 관계. 보낸 요청과 내가 건 제한이 여기 해당한다. */
    fun findAllInitiatedBy(
        userId: UUID,
        status: FriendRelationStatus,
        pageable: Pageable
    ): Slice<FriendRelationJpaEntity> =
        slice(pageable, participates(userId), statusEq(status), relation.initiatedUserId.eq(userId))

    /** 내가 당한 관계. 받은 요청이 여기 해당한다. */
    fun findAllTargetedAt(
        userId: UUID,
        status: FriendRelationStatus,
        pageable: Pageable
    ): Slice<FriendRelationJpaEntity> =
        slice(pageable, participates(userId), statusEq(status), relation.initiatedUserId.ne(userId))

    fun findAllInitiatedByWithStatuses(
        userId: UUID,
        statuses: Collection<FriendRelationStatus>,
        pageable: Pageable
    ): Slice<FriendRelationJpaEntity> =
        slice(pageable, participates(userId), statusIn(statuses), relation.initiatedUserId.eq(userId))
    
    fun existsActiveRestriction(
        lowUserId: UUID,
        highUserId: UUID,
        restrictorId: UUID,
        statuses: Collection<FriendRelationStatus>,
        now: LocalDateTime
    ): Boolean =
        queryFactory.selectOne()
            .from(relation)
            .where(
                relation.lowUserId.eq(lowUserId),
                relation.highUserId.eq(highUserId),
                statusIn(statuses),
                relation.initiatedUserId.eq(restrictorId),
                notExpired(now)
            )
            .fetchFirst() != null

    // --- 조건 -------------------------------------------------------------

    private fun participates(userId: UUID): BooleanExpression =
        relation.lowUserId.eq(userId).or(relation.highUserId.eq(userId))

    private fun statusEq(status: FriendRelationStatus): BooleanExpression = relation.status.eq(status)

    private fun statusIn(statuses: Collection<FriendRelationStatus>): BooleanExpression =
        relation.status.`in`(statuses)

    private fun notExpired(now: LocalDateTime): BooleanExpression =
        relation.expiredAt.isNull.or(relation.expiredAt.gt(now))

    // --- 페이징 -----------------------------------------------------------

    private fun slice(
        pageable: Pageable,
        vararg conditions: BooleanExpression
    ): Slice<FriendRelationJpaEntity> {
        val content = queryFactory.selectFrom(relation)
            .where(*conditions)
            .orderBy(*orderSpecifiers(pageable))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong() + 1)
            .fetch()

        val hasNext = content.size > pageable.pageSize
        return SliceImpl(if (hasNext) content.subList(0, pageable.pageSize) else content, pageable, hasNext)
    }

    @Suppress("UNCHECKED_CAST")
    private fun orderSpecifiers(pageable: Pageable): Array<OrderSpecifier<*>> {
        if (pageable.sort.isUnsorted) return arrayOf(relation.id.asc())

        val path = PathBuilder(relation.type, relation.metadata)
        return pageable.sort
            .map { order ->
                OrderSpecifier(
                    if (order.isAscending) Order.ASC else Order.DESC,
                    path.get(order.property) as Expression<Comparable<Any>>
                )
            }
            .toList()
            .toTypedArray()
    }
}
