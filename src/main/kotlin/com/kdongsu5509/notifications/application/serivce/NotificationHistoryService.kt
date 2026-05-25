package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.application.port.`in`.NotificationHistoryUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.NotificationHistory
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service

@Service
class NotificationHistoryService(
    private val notificationHistoryPersistencePort: NotificationHistoryPersistencePort,
) : NotificationHistoryUseCase {
    override fun findByReceiverEmail(email: String, page: Int, size: Int): List<NotificationHistory> {
        return notificationHistoryPersistencePort.findByReceiverEmail(email, page, size)
    }

    override fun markAsRead(email: String, id: Long) {
        val notification = notificationHistoryPersistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))
        if (notification.receiverEmail != email) NotificationException.NOT_MY_NOTIFICATION.throwIt(contextData = mapOf("id" to id))

        val updatedNotification = notification.markAsRead()
        notificationHistoryPersistencePort.save(updatedNotification)
    }
}
