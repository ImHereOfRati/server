package com.kdongsu5509.user.adapter.out.messageQueue

import com.kdongsu5509.support.config.RabbitMQConfig
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationType
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.out.noti.FriendAlertPort
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class FriendAlertExternalMessageQueueAdapter(val rabbitTemplate: RabbitTemplate) : FriendAlertPort {
    override fun sendAlert(alertInformation: AlertInformation) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.FRIEND_ROUTING_KEY,
            consistNotificationMessageDto(alertInformation)
        )
    }

    private fun consistNotificationMessageDto(alertInformation: AlertInformation): NotificationMessageDto {
        val externalMessageDto = NotificationMessageDto(
            type = NotificationType.FRIEND_REQUEST,
            senderEmail = alertInformation.senderNickname,
            receiverEmail = alertInformation.receiverEmail!!,
            message = alertInformation.body,
            data = mapOf(
                "targetScreen" to "/friend/list"
            )
        )
        return externalMessageDto
    }
}
