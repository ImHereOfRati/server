package com.kdongsu5509.notifications.event

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.shared.event.DomainEvent
import java.time.LocalDateTime
import java.util.UUID

data class NotificationRequested(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent {
    companion object {
        fun from(command: NotificationCommand): NotificationRequested =
            NotificationRequested(
                senderNickname = command.senderNickname,
                senderEmail = command.senderEmail,
                notificationMethod = command.notificationMethod,
                targetIdentifier = command.targetIdentifier,
                type = command.type,
                extraData = command.extraData,
            )

        fun deliveryReceipt(senderEmail: String, type: NotificationType): NotificationRequested =
            from(NotificationCommand.deliveryReceipt(senderEmail, type))
    }
}
