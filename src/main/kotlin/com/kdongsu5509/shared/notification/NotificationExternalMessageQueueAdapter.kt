package com.kdongsu5509.shared.notification

import com.kdongsu5509.shared.notification.dto.NotificationCategory
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
        val routingKey = when (request.category) {
            NotificationCategory.FRIEND_REQUEST_RECEIVED -> RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED
            NotificationCategory.FRIEND_REQUEST_ACCEPTED -> RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_ACCEPTED
            NotificationCategory.ARRIVAL_CONFIRMATION -> RabbitMQConfig.ROUTING_KEY_ARRIVAL_CONFIRMATION
            NotificationCategory.TERMS_UPDATE_NOTICE -> RabbitMQConfig.ROUTING_KEY_TERMS_UPDATE
            NotificationCategory.DELIVERY_RESULT_NOTICE -> RabbitMQConfig.ROUTING_KEY_DELIVERY_RESULT
        }

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            routingKey,
            NotificationQueueMessage.from(request)
        )
    }
}
