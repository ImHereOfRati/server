package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.stereotype.Component

@Component
class FcmTokenMapper {
    fun toDomain(jpaEntity: FcmTokenJpaEntity): FcmToken {
        return FcmToken(
            id = jpaEntity.id,
            email = jpaEntity.email,
            fcmToken = jpaEntity.token,
            deviceType = jpaEntity.deviceType,
            updatedAt = jpaEntity.updatedAt,
        )
    }

    fun toEntity(domain: FcmToken) = FcmTokenJpaEntity(
        token = domain.fcmToken,
        email = domain.email,
        deviceType = domain.deviceType
    ).apply {
        id = domain.id
    }
}
