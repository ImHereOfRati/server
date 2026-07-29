package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.springframework.stereotype.Service

@Service
class FailedNotificationAdminService(
    private val persistencePort: NotificationPersistencePort,
    private val recorder: NotificationRecorder,
    private val deliveryService: NotificationDeliveryService,
) {
    fun findAll(status: NotificationStatus, page: Int, size: Int): List<Notification> =
        persistencePort.findByStatus(status, page, size)

    fun findById(id: Long): Notification = recorder.findById(id)

    fun redeliver(id: Long) {
        val pending = recorder.retry(id)
        deliveryService.redeliver(requireNotNull(pending.id))
    }

    fun redeliver(count: Int?): Int {
        val requested = count?.coerceAtLeast(1)
        val targetIds = mutableListOf<Long>()
        var page = 0

        do {
            val remaining = requested?.minus(targetIds.size)
            if (remaining != null && remaining <= 0) break

            val batch = persistencePort.findByStatus(NotificationStatus.DEAD, page, BATCH_SIZE)
            val selected = remaining?.let(batch::take) ?: batch
            targetIds += selected.map { requireNotNull(it.id) }
            page++
        } while (batch.size == BATCH_SIZE)

        targetIds.forEach(::redeliver)
        return targetIds.size
    }

    fun discard(id: Long) {
        recorder.findById(id).retry()
        recorder.delete(id)
    }

    private companion object {
        const val BATCH_SIZE = 100
    }
}
