package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.service.MessageIdempotencyService
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import org.slf4j.LoggerFactory

/**
 * 알림 메시지 컨슈머 공통 골격.
 *
 * 책임: 멱등성 판정 → 발송 디스패치 → (메타 타입 제외) 발송 결과 알림 enqueue → 처리 완료 마킹.
 * 실패 시의 운영자/요청자 알림은 [ConsumerFailureNotifier]에 위임한다(SRP).
 * 예외는 다시 던져 RabbitMQ NACK → DLQ 흐름을 유지한다.
 */
abstract class AbstractNotificationConsumer(
    private val notificationDispatcherUseCase: NotificationDispatcherUseCase,
    private val messageIdempotencyService: MessageIdempotencyService,
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase,
    private val failureNotifier: ConsumerFailureNotifier
) {
    protected val log = LoggerFactory.getLogger(javaClass)

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
            failureNotifier.notifyFailure(dto, queueLabel, messageId, e)
            throw e
        }

        // 정상 발송 완료 후에만 처리 완료로 기록한다.
        messageIdempotencyService.markAsProcessed(messageId)
    }

    companion object {
        private val META_NOTIFICATION_TYPES =
            setOf(NotificationType.DELIVERY_RESULT_NOTICE, NotificationType.DELIVERY_FAILED_NOTICE)
    }
}
