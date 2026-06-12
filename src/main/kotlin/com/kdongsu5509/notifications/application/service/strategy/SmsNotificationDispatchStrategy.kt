package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.springframework.stereotype.Component

@Component
class SmsNotificationDispatchStrategy(
    private val smsService: MessageSendUseCase
) : NotificationDispatchStrategy {

    override val notificationMethod: NotificationMethod = NotificationMethod.SMS

    override fun dispatch(command: NotificationCommand) {
        smsService.send(
            senderNickname = command.senderNickname,
            receiverNumber = command.targetIdentifier,
            location = command.extraData["location"].orEmpty()
        )
    }

    override fun dispatchMultiple(command: MultipleNotificationCommand) {
        smsService.sendMultiple(
            senderNickname = command.senderNickname,
            receiverNumbers = command.targetIdentifiers,
            location = command.extraData["location"].orEmpty()
        )
    }
}
