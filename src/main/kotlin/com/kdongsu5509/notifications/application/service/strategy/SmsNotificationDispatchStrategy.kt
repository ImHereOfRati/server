package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.springframework.stereotype.Component

@Component
class SmsNotificationDispatchStrategy(
    private val smsService: MessageSendUseCase
) : NotificationDispatchStrategy {

    override val notificationMethod: NotificationMethod = NotificationMethod.SMS

    override fun dispatch(command: NotificationCommand) {
        val body = command.body
            ?: throw InvalidInputException("SMS 본문이 누락되었습니다.")
        smsService.send(
            senderNickname = command.senderNickname,
            receiverNumber = command.targetIdentifier,
            body = body
        )
    }

    override fun dispatchMultiple(command: MultipleNotificationCommand) {
        val body = command.body
            ?: throw InvalidInputException("SMS 본문이 누락되었습니다.")
        smsService.sendMultiple(
            senderNickname = command.senderNickname,
            receiverNumbers = command.targetIdentifiers,
            body = body
        )
    }
}
