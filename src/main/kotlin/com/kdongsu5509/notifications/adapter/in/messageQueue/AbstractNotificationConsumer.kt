package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.service.MessageIdempotencyService
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.slf4j.LoggerFactory

abstract class AbstractNotificationConsumer(
    private val notificationDispatcherUseCase: NotificationDispatcherUseCase,
    private val messageIdempotencyService: MessageIdempotencyService
) {
    protected val log = LoggerFactory.getLogger(javaClass)

    protected fun processMessage(dto: NotificationMessageDto, queueLabel: String) {
        log.info("수신된 메시지 ({}): {}", queueLabel, dto)
        val messageId = dto.messageId.toString()

        if (messageIdempotencyService.isAlreadyProcessed(messageId)) {
            log.info("중복 메시지 감지, 처리 생략 - queue: {}, messageId: {}", queueLabel, messageId)
            return
        }

        // 발송을 시도한다. 예외가 발생하면 markAsProcessed를 호출하지 않고 예외를 전파한다.
        // → RabbitMQ가 NACK 처리하여 DLQ로 이동하고, 이후 replay 시 재시도할 수 있도록 의도된 설계다.
        notificationDispatcherUseCase.dispatch(
            NotificationCommand(
                senderNickname = dto.sender.nickname,
                senderEmail = dto.sender.email,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = dto.receiver.email,
                type = dto.category.name,
                extraData = dto.data ?: emptyMap()
            )
        )

        // 정상 발송 완료 후에만 처리 완료로 기록한다.
        messageIdempotencyService.markAsProcessed(messageId)
    }
}
