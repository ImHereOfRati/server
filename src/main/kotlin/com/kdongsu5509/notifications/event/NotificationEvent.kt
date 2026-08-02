package com.kdongsu5509.notifications.event

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.shared.event.DomainEvent
import java.time.LocalDateTime
import java.util.*

data class NotificationEvent(
    val senderNickname: String,
    val senderId: UUID,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent {
    companion object {
        fun from(command: NotificationCommand): List<NotificationEvent> =
            command.targetIdentifiers.map {
                NotificationEvent(
                    senderNickname = command.senderNickname,
                    senderId = command.senderId,
                    notificationMethod = command.notificationMethod,
                    targetIdentifier = it,
                    type = command.type,
                    extraData = command.extraData,
                )
            }


        fun deliveryReceipt(senderId: UUID, type: NotificationType): NotificationEvent =
            NotificationEvent(
                senderNickname = "ImHere",
                senderId = senderId,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = senderId.toString(),
                type = type,
            )
    }
}
