package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.domain.DeviceType
import java.util.UUID

interface FcmTokenEnrollUseCase {
    fun save(ownerId: UUID, fcmToken: String, deviceType: DeviceType)
}
