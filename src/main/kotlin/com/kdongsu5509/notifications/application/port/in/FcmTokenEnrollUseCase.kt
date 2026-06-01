package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.domain.DeviceType

interface FcmTokenEnrollUseCase {
    fun save(email: String, fcmToken: String, deviceType: DeviceType)
}
