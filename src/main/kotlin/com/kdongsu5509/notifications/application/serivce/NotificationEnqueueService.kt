package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationProducePort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.stereotype.Service

@Service
class NotificationEnqueueService(
    private val notificationProducePort: NotificationProducePort
) : NotificationEnqueueUseCase {

    override fun enqueue(command: NotificationCommand) {
        notificationProducePort.send(command)
    }

    override fun enqueueMultiple(command: MultipleNotificationCommand) {
        command.targetIdentifiers.forEach { targetId ->
            val singleCommand = NotificationCommand(
                senderNickname = command.senderNickname,
                senderEmail = command.senderEmail,
                notificationMethod = command.notificationMethod,
                targetIdentifier = targetId,
                type = command.type,
                extraData = command.extraData
            )
            notificationProducePort.send(singleCommand)
        }
    }
}
