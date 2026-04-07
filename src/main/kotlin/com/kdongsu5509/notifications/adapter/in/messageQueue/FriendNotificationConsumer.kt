package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class FriendNotificationConsumer(
    private val notificationToUserCasePort: NotificationToUserCasePort
) {
    @RabbitListener(queues = [RabbitMQConfig.FRIEND_QUEUE])
    fun receiveMessage(dto: NotificationMessageDto) {
        notificationToUserCasePort.send(dto.receiverEmail, dto.type.name, dto.message)
    }
}
