package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.service.MessageIdempotencyService
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

abstract class AbstractNotificationConsumer(
    private val notificationDispatcherUseCase: NotificationDispatcherUseCase,
    private val messageIdempotencyService: MessageIdempotencyService,
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase,
    private val discordMessageSendPort: DiscordMessageSendPort
) {
    protected val log = LoggerFactory.getLogger(javaClass)

    @Value("\${discord.url.error.server:}")
    private val errorAlertWebhookUrl: String? = null

    protected fun processMessage(dto: NotificationMessageDto, queueLabel: String) {
        log.info("수신된 메시지 ({}): {}", queueLabel, dto)
        val messageId = dto.messageId.toString()

        if (messageIdempotencyService.isAlreadyProcessed(messageId)) {
            log.info("중복 메시지 감지, 처리 생략 - queue: {}, messageId: {}", queueLabel, messageId)
            return
        }

        try {
            // 발송을 시도한다. 예외가 발생하면 markAsProcessed를 호출하지 않고 예외를 전파한다.
            // → RabbitMQ가 NACK 처리하여 DLQ로 이동하고, 이후 replay 시 재시도할 수 있도록 의도된 설계다.
            notificationDispatcherUseCase.dispatch(
                NotificationCommand(
                    senderNickname = dto.sender.nickname,
                    senderEmail = dto.sender.email,
                    notificationMethod = dto.notificationMethod,
                    targetIdentifier = dto.receiver.email,
                    type = dto.category.name,
                    extraData = dto.data ?: emptyMap()
                )
            )

            if (dto.category !in META_NOTIFICATION_TYPES) {
                notificationEnqueueUseCase.enqueue(
                    NotificationCommand(
                        senderNickname = "ImHere",
                        senderEmail = dto.sender.email,
                        notificationMethod = NotificationMethod.FCM,
                        targetIdentifier = dto.sender.email,
                        type = NotificationType.DELIVERY_RESULT_NOTICE.name,
                        extraData = emptyMap()
                    )
                )
            }
        } catch (e: Exception) {
            notifyConsumerFailure(queueLabel, messageId, e)
            notifySenderOfFailure(dto)
            throw e
        }

        // 정상 발송 완료 후에만 처리 완료로 기록한다.
        messageIdempotencyService.markAsProcessed(messageId)
    }

    // HTTP 요청 컨텍스트가 없는 컨슈머 스레드라 GlobalExceptionHandler/AccessLogPrinter를 못 타므로 여기서 직접 보낸다.
    // 예외는 다시 던져 RabbitMQ NACK → DLQ 흐름을 그대로 유지한다.
    private fun notifyConsumerFailure(queueLabel: String, messageId: String, e: Exception) {
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
    // 큐 등록 자체가 실패해도 원래 예외(e)를 가리면 안 되므로 여기서 삼킨다.
    // META_NOTIFICATION_TYPES를 제외하는 이유는 실패 알림 발송이 또 실패해 무한히 재귀 발행되는 걸 막기 위함이다.
    private fun notifySenderOfFailure(dto: NotificationMessageDto) {
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
