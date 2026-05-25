package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand

interface NotificationEnqueueUseCase {
    fun enqueue(command: NotificationCommand)
    fun enqueueMultiple(command: MultipleNotificationCommand)
}
