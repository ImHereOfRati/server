package com.kdongsu5509.user.repository

import com.kdongsu5509.user.domain.User
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toDomain(entity: UserJpaEntity?): User? =
        if (entity == null) null
        else User(
            entity.id,
            entity.email,
            entity.nickname,
            entity.role,
            entity.provider,
            entity.status
        )

    fun toEntity(domain: User): UserJpaEntity = UserJpaEntity(
        domain.email,
        domain.nickname,
        domain.role,
        domain.oauthProvider,
        domain.status
    )
}
