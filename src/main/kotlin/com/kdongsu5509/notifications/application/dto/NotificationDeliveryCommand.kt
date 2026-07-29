package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationRequested
import java.util.UUID

data class NotificationDeliveryCommand(
    val eventId: UUID,
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
) {
    companion object {
        fun from(event: NotificationRequested): NotificationDeliveryCommand =
            NotificationDeliveryCommand(
                eventId = event.eventId,
                senderNickname = event.senderNickname,
                senderEmail = event.senderEmail,
                notificationMethod = event.notificationMethod,
                targetIdentifier = event.targetIdentifier,
                type = event.type,
                extraData = event.extraData,
            )
    }
}
