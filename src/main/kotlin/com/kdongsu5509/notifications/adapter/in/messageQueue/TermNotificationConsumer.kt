package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class TermNotificationConsumer(
    private val notificationToUserCasePort: NotificationToUserCasePort
) {
    @RabbitListener(queues = [RabbitMQConfig.SERVICE_QUEUE])
    fun receiveMessage(dto: NotificationMessageDto) {
        val senderEmail = dto.senderEmail ?: "IMHERE_SERVICE"

        notificationToUserCasePort.send(
            senderNickname = "System",
            senderEmail = senderEmail,
            receiverEmail = dto.receiverEmail,
            type = dto.type.name,
            body = dto.message
        )
    }
}
