package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.support.external.AlertChannel
import com.kdongsu5509.support.external.AlertMessage
import com.kdongsu5509.support.external.ErrorAlertPort
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class NotificationEventListener(
    private val deliveryService: NotificationDeliveryService,
    private val errorAlertPort: ErrorAlertPort,
) {
    @ApplicationModuleListener
    fun handle(event: NotificationEvent) =
        deliveryService.deliver(event)

    @ApplicationModuleListener
    fun handle(event: NotificationDeliveryFailed) =
        errorAlertPort.send(
            AlertChannel.SERVER_ERROR,
            AlertMessage.notificationDeliveryFailure(
                notificationId = event.notificationId,
                target = event.targetIdentifier,
                type = event.notificationType,
                errorType = event.errorType,
                errorMessage = event.errorMessage,
            ),
        )

    @ApplicationModuleListener
    fun handle(origin: FriendRequestSent) =
        deliveryService.deliver(convertToNotificationEvent(origin))

    @ApplicationModuleListener
    fun handle(origin: FriendRequestAccepted) =
        deliveryService.deliver(convertToNotificationEvent(origin))

    private fun convertToNotificationEvent(origin: FriendRequestSent): NotificationEvent = NotificationEvent(
        eventId = origin.eventId,
        senderNickname = origin.requesterNickname,
        senderId = origin.requesterId,
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = origin.receiverId.toString(),
        type = NotificationType.FRIEND_REQUEST_RECEIVED,
    )

    private fun convertToNotificationEvent(origin: FriendRequestAccepted): NotificationEvent = NotificationEvent(
        eventId = origin.eventId,
        senderNickname = origin.accepterNickname,
        senderId = origin.accepterId,
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = origin.requesterId.toString(),
        type = NotificationType.FRIEND_REQUEST_ACCEPTED,
    )
}
