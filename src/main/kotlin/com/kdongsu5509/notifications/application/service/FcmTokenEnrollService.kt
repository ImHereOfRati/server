package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.`in`.FcmTokenEnrollUseCase
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.util.UUID
import com.kdongsu5509.support.logger.SensitiveMasker
import com.kdongsu5509.support.logger.logger

@Service
@Transactional
class FcmTokenEnrollService(
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
) : FcmTokenEnrollUseCase {
    private val log = logger()
    override fun save(ownerId: UUID, fcmToken: String, deviceType: DeviceType) {
        val existingToken = fcmTokenPersistencePort.findByOwnerId(ownerId)

        if (existingToken != null) {
            log.info("FCM 토큰 갱신: ownerId={}, deviceType={}, token={}", ownerId, deviceType, SensitiveMasker.token(fcmToken))
            val updatedToken = existingToken.update(fcmToken, deviceType)
            fcmTokenPersistencePort.save(updatedToken)
            return
        }

        val newFcmToken = FcmToken(ownerId = ownerId, fcmToken = fcmToken, deviceType = deviceType)
        log.info("FCM 토큰 신규 등록: ownerId={}, deviceType={}, token={}", ownerId, deviceType, SensitiveMasker.token(fcmToken))
        fcmTokenPersistencePort.save(newFcmToken)
    }
}
