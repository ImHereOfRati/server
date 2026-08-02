package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.RenderedNotification

interface FirebasePort {
    fun send(fcmToken: String, deviceType: DeviceType, rendered: RenderedNotification)
}
