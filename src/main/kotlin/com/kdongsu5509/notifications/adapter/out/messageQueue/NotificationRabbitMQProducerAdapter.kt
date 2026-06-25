package com.kdongsu5509.notifications.adapter.out.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.out.NotificationProducePort
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NotificationRabbitMQProducerAdapter(
    private val rabbitTemplate: RabbitTemplate
) : NotificationProducePort {

    override fun send(command: NotificationCommand) {
        val category = NotificationType.valueOf(command.type)
        val messageDto = NotificationMessageDto(
            category = category,
            sender = NotificationPersonInfo(command.senderEmail, command.senderNickname),
            receiver = NotificationPersonInfo(command.targetIdentifier, ""),
            notificationMethod = command.notificationMethod,
            data = command.extraData,
            messageId = UUID.randomUUID()
        )

        val routingKey = findProperRoutingKey(category)

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            routingKey,
            messageDto
        )
    }

    private fun findProperRoutingKey(category: NotificationType): String {
        return when (category) {
            NotificationType.FRIEND_REQUEST_RECEIVED -> RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED
            NotificationType.FRIEND_REQUEST_ACCEPTED -> RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_ACCEPTED
            NotificationType.LOCATION_SHARE_RECEIVED -> RabbitMQConfig.ROUTING_KEY_LOCATION_SHARE
            NotificationType.ARRIVAL, NotificationType.ARRIVAL_CONFIRMATION -> RabbitMQConfig.ROUTING_KEY_ARRIVAL_CONFIRMATION
            NotificationType.DEPARTURE -> RabbitMQConfig.ROUTING_KEY_DEPARTURE_CONFIRMATION
            NotificationType.TERMS_UPDATE_NOTICE -> RabbitMQConfig.ROUTING_KEY_TERMS_UPDATE
            NotificationType.DELIVERY_RESULT_NOTICE -> RabbitMQConfig.ROUTING_KEY_DELIVERY_RESULT
            NotificationType.DELIVERY_FAILED_NOTICE -> RabbitMQConfig.ROUTING_KEY_DELIVERY_FAILED
        }
    }
}
