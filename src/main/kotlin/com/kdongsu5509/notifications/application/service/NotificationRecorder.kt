package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationDeliveryCommand
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class NotificationRecorder(
    private val persistencePort: NotificationPersistencePort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reserve(command: NotificationDeliveryCommand): Notification? {
        val dedupeKey = Notification.dedupeKeyOf(command.eventId, command.notificationMethod)
        persistencePort.findByDedupeKey(dedupeKey)?.let { existing ->
            return existing.takeIf { it.status == NotificationStatus.FAILED }
        }

        val rendered = command.type.render(
            senderNickname = command.senderNickname,
            senderEmail = command.senderEmail,
            extraData = command.extraData,
        )
        val bodyOverride = if (command.notificationMethod == NotificationMethod.SMS) {
            command.extraData[NotificationCommand.BODY_KEY]
                ?.takeIf(String::isNotBlank)
                ?: throw InvalidInputException("SMS 본문이 누락되었습니다.")
        } else {
            null
        }

        return persistencePort.save(
            Notification.request(
                dedupeKey = dedupeKey,
                targetIdentifier = command.targetIdentifier,
                method = command.notificationMethod,
                senderEmail = command.senderEmail,
                rendered = rendered,
                bodyOverride = bodyOverride,
            )
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findById(id: Long): Notification =
        persistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findByDedupeKey(dedupeKey: String): Notification? =
        persistencePort.findByDedupeKey(dedupeKey)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markSent(id: Long): Notification =
        persistencePort.save(findById(id).markSent(LocalDateTime.now()))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun markFailed(id: Long, error: Throwable): Notification =
        persistencePort.save(findById(id).markFailed(error.message ?: error.javaClass.simpleName))

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun retry(id: Long): Notification =
        persistencePort.save(findById(id).retry())

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun delete(id: Long) {
        persistencePort.deleteById(id)
    }
}
