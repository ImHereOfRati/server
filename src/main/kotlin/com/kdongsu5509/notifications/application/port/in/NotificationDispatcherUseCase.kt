package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand

interface NotificationDispatcherUseCase {
    fun dispatch(command: NotificationCommand)
    fun dispatchMultiple(command: MultipleNotificationCommand)
}
