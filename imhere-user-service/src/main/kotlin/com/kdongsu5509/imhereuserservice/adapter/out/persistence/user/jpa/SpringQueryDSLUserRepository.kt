package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa

import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class SpringQueryDSLUserRepository(private val queryFactory: JPAQueryFactory) {

    private val user = QUserJpaEntity.userJpaEntity

    fun findUserByEmail(email: String): Optional<UserJpaEntity> {
        val result = queryFactory.selectFrom(user)
            .where(isEmailMatching(email))
            .fetchOne()

        return Optional.ofNullable(result)
    }

    fun findActiveUserByEmail(email: String): Optional<UserJpaEntity> {
        val result = queryFactory.selectFrom(user)
            .where(
                isEmailMatching(email), isActive()
            )
            .fetchOne()

        return Optional.ofNullable(result)
    }

    fun findUserByKeyword(keyword: String): List<UserJpaEntity> {
        return queryFactory.selectFrom(user)
            .where(
                isNameMatching(keyword)
                    .or(isEmailMatching(keyword))
            )
            .fetch()
    }

    private fun isEmailMatching(keyword: String): BooleanExpression = user.email.eq(keyword)

    private fun isNameMatching(keyword: String): BooleanExpression = user.nickname.eq(keyword)

    private fun isActive(): BooleanExpression? = user.status.eq(UserStatus.ACTIVE)
}