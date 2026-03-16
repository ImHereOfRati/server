package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import org.springframework.stereotype.Component

@Component
class FcmTokenMapper {
    fun mapToJpaEntity(fcmToken: FcmToken, user: UserJpaEntity, deviceType: DeviceType): FcmTokenJpaEntity {
        return FcmTokenJpaEntity(
            token = fcmToken.fcmToken,
            user = user,
            deviceType = deviceType
        )
    }

    fun mapToDomainEntity(jpaEntity: FcmTokenJpaEntity): FcmToken {
        return FcmToken(
            id = jpaEntity.id,
            userEmail = jpaEntity.user.email,
            fcmToken = jpaEntity.token,
            deviceType = jpaEntity.deviceType,
            updatedAt = jpaEntity.updatedAt,
        )
    }
}