package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class NotificationFailureNotifier(
    private val receiptPublisher: NotificationReceiptPublisher,
    private val discordMessageSendPort: DiscordMessageSendPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${discord.url.error.server:}")
    private val errorAlertWebhookUrl: String? = null

    fun notifyFailure(notification: Notification, error: Throwable) {
        runCatching {
            errorAlertWebhookUrl
                ?.takeIf(String::isNotBlank)
                ?.let { webhook ->
                    discordMessageSendPort.sendMessage(
                        webhook,
                        DiscordMessageDto(
                            """
                            ## 💥 Notification Delivery Failure
                            **NotificationId:** `${notification.id}`
                            **Target:** `${notification.targetIdentifier}`
                            **Type:** `${notification.type}`
                            **Error:** ${error.javaClass.simpleName} - ${error.message}
                            """.trimIndent()
                        ),
                    )
                }
        }.onFailure { log.error("발송 실패 Discord 알림 중 오류", it) }

        runCatching {
            receiptPublisher.publish(notification, NotificationType.DELIVERY_FAILED_NOTICE)
        }.onFailure { log.error("발송 실패 알림 이벤트 발행 중 오류", it) }
    }
}
