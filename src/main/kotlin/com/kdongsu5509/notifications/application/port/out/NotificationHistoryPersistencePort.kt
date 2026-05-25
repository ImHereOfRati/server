package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.NotificationHistory

interface NotificationHistoryPersistencePort {
    fun save(notificationHistory: NotificationHistory): NotificationHistory
    fun findById(id: Long): NotificationHistory?
    fun findByReceiverEmail(receiverEmail: String, page: Int, size: Int): List<NotificationHistory>
}
