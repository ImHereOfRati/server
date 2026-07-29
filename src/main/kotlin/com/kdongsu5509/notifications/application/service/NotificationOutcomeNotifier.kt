package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationRequested
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class NotificationOutcomeNotifier(
    private val eventPublisher: DomainEventPublisher,
    private val discordMessageSendPort: DiscordMessageSendPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${discord.url.error.server:}")
    private val errorAlertWebhookUrl: String? = null

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun success(notification: Notification) {
        publishReceipt(notification, NotificationType.DELIVERY_RESULT_NOTICE)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun failure(notification: Notification, error: Throwable) {
        sendDiscordAlert(notification, error)
        publishReceipt(notification, NotificationType.DELIVERY_FAILED_NOTICE)
    }

    private fun publishReceipt(notification: Notification, type: NotificationType) {
        if (notification.type.isMeta) return
        eventPublisher.publish(NotificationRequested.deliveryReceipt(notification.senderEmail, type))
    }

    private fun sendDiscordAlert(notification: Notification, error: Throwable) {
        runCatching {
            errorAlertWebhookUrl
                ?.takeIf(String::isNotBlank)
                ?.let { webhook ->
                    discordMessageSendPort.sendMessage(
                        webhook,
                        DiscordMessageDto(
                            """
                            ## 🚨 Notification Delivery Failure
                            **NotificationId:** `${notification.id}`
                            **Target:** `${notification.targetIdentifier}`
                            **Type:** `${notification.type}`
                            **Error:** ${error.javaClass.simpleName} - ${error.message}
                            """.trimIndent()
                        ),
                    )
                }
        }.onFailure { log.error("알림 발송 실패 Discord 통지 중 오류", it) }
    }
}
