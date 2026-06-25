package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.service.MessageIdempotencyService
import com.kdongsu5509.support.config.RabbitMQConfig
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class FriendNotificationConsumer(
    notificationDispatcherUseCase: NotificationDispatcherUseCase,
    messageIdempotencyService: MessageIdempotencyService,
    notificationEnqueueUseCase: NotificationEnqueueUseCase,
    discordMessageSendPort: DiscordMessageSendPort
) : AbstractNotificationConsumer(notificationDispatcherUseCase, messageIdempotencyService, notificationEnqueueUseCase, discordMessageSendPort) {
    companion object {
        const val QUEUE_LABEL = "FRIEND"
    }

    @RabbitListener(queues = [RabbitMQConfig.FRIEND_QUEUE])
    fun receiveMessage(dto: NotificationMessageDto) = processMessage(dto, QUEUE_LABEL)
}
