package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.application.port.out.SenderAliasPort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType.FRIEND_REQUEST_ACCEPTED
import com.kdongsu5509.notifications.domain.NotificationType.FRIEND_REQUEST_RECEIVED
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class NotificationRegister(
    private val notificationPersistencePort: NotificationPersistencePort,
    private val senderAliasPort: SenderAliasPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun findByDedupeKey(dedupeKey: String): Notification? = notificationPersistencePort.findByDedupeKey(dedupeKey)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun register(event: NotificationEvent): Notification =
        notificationPersistencePort.save(
            Notification.request(
                dedupeKey = Notification.dedupeKeyOf(event.eventId, event.notificationMethod),
                targetIdentifier = event.targetIdentifier,
                method = event.notificationMethod,
                rendered = NotificationTemplate.render(
                    type = event.type,
                    senderAlias = resolveSenderAlias(event),
                    extraData = event.extraData,
                ),
                bodyOverride = event.bodyOverride(),
            )
        )

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    fun getByIdOrThrow(id: Long): Notification =
        notificationPersistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markAsSent(id: Long): Notification =
        notificationPersistencePort.save(getByIdOrThrow(id).markSent(LocalDateTime.now()))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(id: Long, reason: String): Notification =
        notificationPersistencePort.save(getByIdOrThrow(id).markFailed(reason))

    private fun resolveSenderAlias(event: NotificationEvent): String {
        if (event.type == FRIEND_REQUEST_RECEIVED || event.type == FRIEND_REQUEST_ACCEPTED) {
            return event.senderNickname
        }

        val ownerId = event.targetUserId ?: return event.senderNickname

        return try {
            senderAliasPort.findAlias(ownerId = ownerId, senderId = event.senderId) ?: event.senderNickname
        } catch (error: Exception) {
            log.warn("발송자 별칭 조회 실패. 닉네임으로 대체한다.", error)
            event.senderNickname
        }
    }
}
