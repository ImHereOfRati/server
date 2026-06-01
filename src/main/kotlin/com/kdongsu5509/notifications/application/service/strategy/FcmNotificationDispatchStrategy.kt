package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.springframework.stereotype.Component

@Component
class FcmNotificationDispatchStrategy(
    private val fcmNotificationService: NotificationUseCase
) : NotificationDispatchStrategy {

    override val notificationMethod: NotificationMethod = NotificationMethod.FCM

    override fun dispatch(command: NotificationCommand) {
        fcmNotificationService.send(
            senderNickname = command.senderNickname,
            senderEmail = command.senderEmail,
            receiverEmail = command.targetIdentifier,
            type = command.type,
            extraData = command.extraData
        )
    }

    override fun dispatchMultiple(command: MultipleNotificationCommand) {
        command.targetIdentifiers.forEach { targetId ->
            fcmNotificationService.send(
                senderNickname = command.senderNickname,
                senderEmail = command.senderEmail,
                receiverEmail = targetId,
                type = command.type,
                extraData = command.extraData
            )
        }
    }
}
