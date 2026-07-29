package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.`in`.NotificationInboxUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationInboxService(
    private val persistencePort: NotificationPersistencePort,
) : NotificationInboxUseCase {
    @Transactional(readOnly = true)
    override fun findByReceiverEmail(email: String, page: Int, size: Int): List<Notification> =
        persistencePort.findInbox(email, page, size)

    @Transactional
    override fun markAsRead(email: String, id: Long) {
        val notification = persistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))
        if (notification.targetIdentifier != email) {
            NotificationException.NOT_MY_NOTIFICATION.throwIt(contextData = mapOf("id" to id))
        }
        persistencePort.save(notification.markAsRead())
    }
}
