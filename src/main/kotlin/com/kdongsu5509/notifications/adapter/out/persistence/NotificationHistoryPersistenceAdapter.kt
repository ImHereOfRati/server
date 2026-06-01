package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.NotificationHistory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class NotificationHistoryPersistenceAdapter(
    private val repository: SpringDataNotificationHistoryRepository,
    private val mapper: NotificationHistoryMapper
) : NotificationHistoryPersistencePort {

    override fun save(notificationHistory: NotificationHistory): NotificationHistory {
        val entity = mapper.toEntity(notificationHistory)
        val savedEntity = repository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): NotificationHistory? {
        return repository.findById(id).orElse(null)?.let { mapper.toDomain(it) }
    }

    override fun findByReceiverEmail(receiverEmail: String, page: Int, size: Int): List<NotificationHistory> {
        val pageable = PageRequest.of(page, size)
        return repository.findByReceiverEmailOrderByCreatedAtDesc(receiverEmail, pageable)
            .content
            .map { mapper.toDomain(it) }
    }
}
