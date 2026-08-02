package com.kdongsu5509.user.repository.jpa

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

    fun findAllActiveByKeyword(
        keyword: String,
        excludedUserIds: Set<UUID>,
        pageable: Pageable = PageRequest.of(0, 20)
    ): Slice<UserJpaEntity> {
        val content = queryFactory.selectFrom(user)
            .where(
                nicknameEqualsOrEmailEquals(keyword),
                isActive(),
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

    private fun emailEquals(email: String): BooleanExpression = user.email.eq(email)
    private fun isActive(): BooleanExpression = user.status.eq(UserStatus.ACTIVE)

    private fun nicknameEqualsOrEmailEquals(keyword: String): BooleanExpression =
        if (keyword.contains("@")) {
            emailEquals(keyword)
        } else {
            user.nickname.eq(keyword)
        }
}
