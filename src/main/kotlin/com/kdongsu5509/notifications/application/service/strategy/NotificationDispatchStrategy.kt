package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod

interface NotificationDispatchStrategy {
    val notificationMethod: NotificationMethod
    fun dispatch(command: NotificationCommand)
    fun dispatchMultiple(command: MultipleNotificationCommand)
}
