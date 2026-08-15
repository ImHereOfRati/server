package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FcmTokenPersistenceAdapter(
    private val fcmTokenMapper: FcmTokenMapper,
    private val springDataFcmTokenRepository: SpringDataFcmTokenRepository,
) : FcmTokenPersistencePort {

    override fun save(fcmToken: FcmToken) {
        springDataFcmTokenRepository.save(
            fcmTokenMapper.toEntity(fcmToken)
        )
    }

    override fun findByOwnerId(ownerId: UUID): FcmToken? {
        return springDataFcmTokenRepository.findByOwnerId(ownerId)
            ?.let { fcmTokenMapper.toDomain(it) }
    }

    override fun deleteByOwnerId(ownerId: UUID) {
        springDataFcmTokenRepository.deleteByOwnerId(ownerId)
    }

    override fun deleteById(fcmTokenId: Long) {
        springDataFcmTokenRepository.deleteById(fcmTokenId)
    }
}
