package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.stereotype.Component

@Component
class FcmTokenMapper {
    fun toDomain(jpaEntity: FcmTokenJpaEntity): FcmToken =
        FcmToken(
            id = jpaEntity.id,
            ownerId = jpaEntity.ownerId,
            fcmToken = jpaEntity.token,
            deviceType = jpaEntity.deviceType,
            createdAt = jpaEntity.createdAt,
            updatedAt = jpaEntity.updatedAt,
        )

    fun toEntity(domain: FcmToken) = FcmTokenJpaEntity(
        token = domain.fcmToken,
        ownerId = domain.ownerId,
        deviceType = domain.deviceType
    ).apply {
        id = domain.id
    }
}
