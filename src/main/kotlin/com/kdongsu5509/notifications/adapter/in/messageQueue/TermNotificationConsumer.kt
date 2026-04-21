package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.notifications.application.serivce.MessageIdempotencyService
import com.kdongsu5509.support.config.RabbitMQConfig
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class TermNotificationConsumer(
    private val notificationToUserCasePort: NotificationToUserCasePort,
    private val messageIdempotencyService: MessageIdempotencyService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitMQConfig.SERVICE_QUEUE])
    fun receiveMessage(dto: NotificationMessageDto) {
        val messageId = dto.messageId.toString()

        if (messageIdempotencyService.isAlreadyProcessed(messageId)) {
            log.info("중복 메시지 감지, 처리 생략 - messageId: {}", messageId)
            return
        }

        val senderEmail = dto.senderEmail ?: "IMHERE_SERVICE"

        notificationToUserCasePort.send(
            senderNickname = "System",
            senderEmail = senderEmail,
            receiverEmail = dto.receiverEmail,
            type = dto.type.name,
            body = dto.message,
            extraData = dto.data ?: emptyMap()
        )

        messageIdempotencyService.markAsProcessed(messageId)
    }
}
