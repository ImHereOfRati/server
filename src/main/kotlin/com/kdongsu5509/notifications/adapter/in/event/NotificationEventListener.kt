package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.application.service.NotificationDeliveryFacade
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.support.external.AlertChannel
import com.kdongsu5509.support.external.AlertMessage
import com.kdongsu5509.support.external.ErrorAlertPort
import com.kdongsu5509.shared.event.UserWithdrawnEvent
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class NotificationEventListener(
    private val deliveryFacade: NotificationDeliveryFacade,
    private val errorAlertPort: ErrorAlertPort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val notificationPersistencePort: NotificationPersistencePort,
) {
    @ApplicationModuleListener
    fun handle(event: NotificationEvent) =
        deliveryFacade.deliver(event)

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
        deliveryFacade.deliver(convertToNotificationEvent(origin))

    @ApplicationModuleListener
    fun handle(origin: FriendRequestAccepted) =
        deliveryFacade.deliver(convertToNotificationEvent(origin))

    @ApplicationModuleListener
    fun handle(event: UserWithdrawnEvent) {
        fcmTokenPersistencePort.deleteByOwnerId(event.userId)
        notificationPersistencePort.deleteByTargetIdentifier(event.userId.toString())
    }

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
