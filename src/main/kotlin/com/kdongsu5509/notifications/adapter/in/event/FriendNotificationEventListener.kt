package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.application.dto.NotificationDeliveryCommand
import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class FriendNotificationEventListener(
    private val deliveryService: NotificationDeliveryService,
) {
    @ApplicationModuleListener
    fun handle(event: FriendRequestSent) {
        deliveryService.deliver(
            NotificationDeliveryCommand(
                eventId = event.eventId,
                senderNickname = event.requesterNickname,
                senderEmail = event.requesterEmail,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = event.receiverEmail,
                type = NotificationType.FRIEND_REQUEST_RECEIVED,
            )
        )
    }

    @ApplicationModuleListener
    fun handle(event: FriendRequestAccepted) {
        deliveryService.deliver(
            NotificationDeliveryCommand(
                eventId = event.eventId,
                senderNickname = event.accepterNickname,
                senderEmail = event.accepterEmail,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = event.requesterEmail,
                type = NotificationType.FRIEND_REQUEST_ACCEPTED,
            )
        )
    }
}
