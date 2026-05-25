package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.application.dto.NotificationCommand

interface NotificationProducePort {
    fun send(command: NotificationCommand)
}
