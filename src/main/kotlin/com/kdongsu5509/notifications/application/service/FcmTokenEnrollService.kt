package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.`in`.FcmTokenEnrollUseCase
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class FcmTokenEnrollService(
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
) : FcmTokenEnrollUseCase {
    override fun save(email: String, fcmToken: String, deviceType: DeviceType) {
        val existingToken = fcmTokenPersistencePort.findByUserEmail(email)

        if (existingToken != null) {
            val updatedToken = existingToken.update(fcmToken)
            fcmTokenPersistencePort.save(updatedToken)
            return
        }

        val newFcmToken = FcmToken.create(email = email, fcmToken = fcmToken, deviceType = deviceType)
        fcmTokenPersistencePort.save(newFcmToken)
    }
}
