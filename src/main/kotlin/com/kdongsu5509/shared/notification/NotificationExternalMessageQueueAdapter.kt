package com.kdongsu5509.shared.notification

import com.kdongsu5509.shared.notification.dto.NotificationQueueMessage
import com.kdongsu5509.shared.notification.dto.NotificationSendRequest
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class NotificationExternalMessageQueueAdapter(
    private val rabbitTemplate: RabbitTemplate
) : NotificationPort {

    override fun send(request: NotificationSendRequest) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            request.category.routingKey,
            NotificationQueueMessage.from(request)
        )
    }
}
