package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationProducePort
import org.springframework.stereotype.Service

@Service
class NotificationEnqueueService(
    private val notificationProducePort: NotificationProducePort
) : NotificationEnqueueUseCase {

    override fun enqueue(command: NotificationCommand) {
        notificationProducePort.send(command)
    }

    override fun enqueueMultiple(command: MultipleNotificationCommand) {
        command.toSingles().forEach { notificationProducePort.send(it) }
    }
}
