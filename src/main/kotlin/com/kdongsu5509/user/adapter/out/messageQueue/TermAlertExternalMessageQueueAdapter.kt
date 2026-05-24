package com.kdongsu5509.user.adapter.out.messageQueue

import com.kdongsu5509.notifications.config.NotificationRabbitMQConfig
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationType
import com.kdongsu5509.user.application.port.TermAlertPort
import com.kdongsu5509.user.service.dto.AlertResponse
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class TermAlertExternalMessageQueueAdapter(val rabbitTemplate: RabbitTemplate) : TermAlertPort {
    override fun sendAlert(alertResponse: AlertResponse) {
        rabbitTemplate.convertAndSend(
            NotificationRabbitMQConfig.EXCHANGE_NAME,
            NotificationRabbitMQConfig.SERVICE_ROUTING_KEY,
            consistNotificationMessageDto(alertResponse)
        )
    }

    private fun consistNotificationMessageDto(alertResponse: AlertResponse): NotificationMessageDto {
        val externalMessageDto = NotificationMessageDto(
            type = NotificationType.TERMS_UPDATE,
            senderEmail = alertResponse.senderNickname,
            receiverEmail = alertResponse.receiverEmail,
            message = alertResponse.body,
            data = mapOf(
                "targetScreen" to "/friend/list"
            )
        )
        return externalMessageDto
    }
}
