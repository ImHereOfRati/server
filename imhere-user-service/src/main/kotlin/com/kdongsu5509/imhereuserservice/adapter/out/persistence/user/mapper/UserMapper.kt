package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun mapToJpaEntity(domainUser: User): UserJpaEntity {
        return UserJpaEntity(
            domainUser.email,
            domainUser.nickname,
            domainUser.role,
            domainUser.oauthProvider,
            domainUser.status
        )
    }

    fun mapToDomainEntity(jpaEntity: UserJpaEntity): User {
        return User(
            jpaEntity.id,
            jpaEntity.email,
            jpaEntity.nickname,
            jpaEntity.provider,
            jpaEntity.role,
            jpaEntity.status
        )
    }
}