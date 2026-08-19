package com.kdongsu5509.notifications.scheduler

import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.application.service.NotificationDeliveryFacade
import com.kdongsu5509.notifications.application.service.NotificationRegister
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.DeliveryCertainty
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationRecoveryScheduler(
    private val persistencePort: NotificationPersistencePort,
    private val deliveryFacade: NotificationDeliveryFacade,
    private val notificationRegister: NotificationRegister,
    private val externalMessagePort: ExternalMessagePort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notifications.recovery.fixed-delay-ms:60000}")
    fun recoverStalledNotifications() {
        val threshold = LocalDateTime.now().minusMinutes(5)
        persistencePort.recoverStalled(LocalDateTime.now().minusMinutes(10))
        persistencePort.findRecoverable(threshold, 100).forEach { notification ->
            try {
                deliveryFacade.redeliver(requireNotNull(notification.id))
            } catch (error: Exception) {
                log.error("방치 알림 회수 실패 - notificationId={}", notification.id, error)
            }
        }
        reconcileUnknownNotifications()
    }

    private fun reconcileUnknownNotifications() {
        persistencePort.findByStatus(NotificationStatus.UNKNOWN, 0, 100).forEach { notification ->
            val providerMessageId = notification.providerMessageId ?: return@forEach
            val result = externalMessagePort.findStatus(providerMessageId) ?: return@forEach
            when (result.certainty) {
                DeliveryCertainty.CONFIRMED -> notificationRegister.markAsSent(notification.id!!, result)
                DeliveryCertainty.REJECTED -> notificationRegister.markFailed(notification.id!!, result.message)
                DeliveryCertainty.UNKNOWN -> Unit
            }
        }
    }
}
