package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val persistencePort: NotificationPersistencePort,
    private val deliveryFacade: NotificationDeliveryFacade,
    private val eventPublisher: DomainEventPublisher,
    private val smsDailyRecipientRateLimiter: SmsDailyRecipientRateLimiter,
) : NotificationUseCase {

    @Transactional
    override fun requestDelivery(command: NotificationCommand) {
        if (command.notificationMethod == NotificationMethod.SMS) {
            smsDailyRecipientRateLimiter.reserve(command.senderId, command.targetIdentifiers)
        }
        NotificationEvent.from(command)
            .forEach(eventPublisher::publish)
    }

    override fun findByRecipientId(recipientId: UUID, page: Int, size: Int): List<Notification> =
        persistencePort.findInbox(recipientId, page, size)

    @Transactional
    override fun markAsRead(recipientId: UUID, id: Long) {
        val notification = findById(id)
        if (notification.targetIdentifier != recipientId.toString()) {
            NotificationException.NOT_MY_NOTIFICATION.throwIt(contextData = mapOf("id" to id))
        }
        persistencePort.save(notification.markAsRead())
    }

    // --- 관리자 -------------------------------------------------------------

    override fun findAll(status: NotificationStatus, page: Int, size: Int): List<Notification> =
        persistencePort.findByStatus(status, page, size)


    override fun findById(id: Long): Notification =
        persistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))

    // 재발송은 외부 채널 호출을 포함하므로 트랜잭션을 걸치지 않는다. 클래스 레벨의 readOnly를
    // 그대로 물려받으면 되살리기가 읽기 전용 트랜잭션 안에서 일어난다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun redeliver(id: Long) {
        val revived = persistencePort.save(findById(id).retry())
        deliveryFacade.redeliver(requireNotNull(revived.id))
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun redeliverAll(count: Int?): Int {
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

    @Transactional
    override fun discard(id: Long) {
        findById(id).retry()
        persistencePort.deleteById(id)
    }

    private companion object {
        const val BATCH_SIZE = 100
    }
}
