package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class NotificationService(
    private val persistencePort: NotificationPersistencePort,
    private val deliveryService: NotificationDeliveryService,
    private val eventPublisher: DomainEventPublisher,
) : NotificationUseCase {

    // --- 발송 요청 -----------------------------------------------------------

    // 발송 이벤트를 받는 쪽은 @ApplicationModuleListener(= @TransactionalEventListener)라,
    // 활성 트랜잭션의 커밋 시점에만 깨어난다. 트랜잭션 없이 발행하면 이벤트는 조용히 버려지고
    // event_publication에도 남지 않아 재발행 대상조차 되지 않는다. 그래서 여기서 경계를 연다.
    @Transactional
    override fun request(command: NotificationCommand) {
        NotificationEvent.from(command).forEach(eventPublisher::publish)
    }

    // --- 수신자 -------------------------------------------------------------

    @Transactional(readOnly = true)
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

    // --- 운영자 -------------------------------------------------------------

    @Transactional(readOnly = true)
    override fun findAll(status: NotificationStatus, page: Int, size: Int): List<Notification> =
        persistencePort.findByStatus(status, page, size)

    @Transactional(readOnly = true)
    override fun findById(id: Long): Notification =
        persistencePort.findById(id)
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))

    override fun redeliver(id: Long) {
        val revived = retry(id)
        deliveryService.redeliver(requireNotNull(revived.id))
    }

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
        // 되살릴 수 있는 상태인지로 폐기 가능 여부를 가른다. 되살린 값 자체는 버린다.
        findById(id).retry()
        persistencePort.deleteById(id)
    }

    @Transactional
    fun retry(id: Long): Notification = persistencePort.save(findById(id).retry())

    private companion object {
        const val BATCH_SIZE = 100
    }
}
