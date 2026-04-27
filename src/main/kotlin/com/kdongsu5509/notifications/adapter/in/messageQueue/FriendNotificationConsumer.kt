package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.notifications.application.serivce.MessageIdempotencyService
import com.kdongsu5509.support.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class FriendNotificationConsumer(
    private val notificationToUserCasePort: NotificationToUserCasePort,
    private val messageIdempotencyService: MessageIdempotencyService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitMQConfig.FRIEND_QUEUE])
    fun receiveMessage(dto: NotificationMessageDto) {
        val messageId = dto.messageId.toString()

        if (messageIdempotencyService.isAlreadyProcessed(messageId)) {
            return
        }

        val senderEmail = dto.senderEmail ?: throw IllegalStateException("친구 요청 메시지의 senderEmail이 null일 수는 없습니다")

        notificationToUserCasePort.send(
            senderNickname = senderEmail,
            senderEmail = senderEmail,
            receiverEmail = dto.receiverEmail,
            type = dto.type.name,
            body = dto.message
        )

        messageIdempotencyService.markAsProcessed(messageId)
    }
}
