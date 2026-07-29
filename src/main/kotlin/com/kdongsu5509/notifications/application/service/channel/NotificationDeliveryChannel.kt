package com.kdongsu5509.notifications.application.service.channel

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod

interface NotificationDeliveryChannel {
    val method: NotificationMethod
    fun send(notification: Notification)
}
