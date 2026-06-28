package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 컨슈머 처리 실패 시의 알림 책임 전담 협력자.
 *
 * 컨슈머(AbstractNotificationConsumer)에서 발송/멱등성 제어 흐름과 분리해
 * (1) 운영자용 Discord 경보, (2) 발송 요청자(sender)용 FCM 실패 알림을 담당한다.
 * 원래 예외 전파(RabbitMQ NACK → DLQ) 흐름은 호출 측이 유지한다.
 */
@Component
class ConsumerFailureNotifier(
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase,
    private val discordMessageSendPort: DiscordMessageSendPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${discord.url.error.server:}")
    private val errorAlertWebhookUrl: String? = null

    /** 처리 실패를 운영자(Discord)와 발송 요청자(FCM) 양쪽에 알린다. */
    fun notifyFailure(dto: NotificationMessageDto, queueLabel: String, messageId: String, e: Exception) {
        notifyOperatorViaDiscord(queueLabel, messageId, e)
        notifySenderViaFcm(dto)
    }

    // HTTP 요청 컨텍스트가 없는 컨슈머 스레드라 GlobalExceptionHandler/AccessLogPrinter를 못 타므로 여기서 직접 보낸다.
    private fun notifyOperatorViaDiscord(queueLabel: String, messageId: String, e: Exception) {
        errorAlertWebhookUrl?.takeIf { it.isNotEmpty() }?.let { webhookUrl ->
            val content = """
                ## 💥 Background Job Failure
                **Queue:** `$queueLabel`
                **MessageId:** `$messageId`
                **Error:** ${e.javaClass.simpleName} - ${e.message}
            """.trimIndent()
            discordMessageSendPort.sendMessage(webhookUrl, DiscordMessageDto(content))
        }
    }

    // 발송 요청자(sender)는 운영자용 Discord 알림을 볼 수 없으므로 FCM으로 실패를 알린다.
    // 큐 등록 자체가 실패해도 원래 예외를 가리면 안 되므로 여기서 삼킨다.
    // META_NOTIFICATION_TYPES를 제외하는 이유는 실패 알림 발송이 또 실패해 무한히 재귀 발행되는 걸 막기 위함이다.
    private fun notifySenderViaFcm(dto: NotificationMessageDto) {
        if (dto.category in META_NOTIFICATION_TYPES) return

        runCatching {
            notificationEnqueueUseCase.enqueue(
                NotificationCommand(
                    senderNickname = "ImHere",
                    senderEmail = dto.sender.email,
                    notificationMethod = NotificationMethod.FCM,
                    targetIdentifier = dto.sender.email,
                    type = NotificationType.DELIVERY_FAILED_NOTICE.name,
                    extraData = emptyMap()
                )
            )
        }.onFailure { log.error("발송 실패 알림(FCM) 큐 등록 중 오류", it) }
    }

    companion object {
        private val META_NOTIFICATION_TYPES =
            setOf(NotificationType.DELIVERY_RESULT_NOTICE, NotificationType.DELIVERY_FAILED_NOTICE)
    }
}
