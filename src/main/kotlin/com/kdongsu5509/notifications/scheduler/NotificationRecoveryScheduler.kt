package com.kdongsu5509.notifications.scheduler

import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class NotificationRecoveryScheduler(
    private val persistencePort: NotificationPersistencePort,
    private val deliveryService: NotificationDeliveryService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${notifications.recovery.fixed-delay-ms:60000}")
    fun recoverStalledNotifications() {
        val threshold = LocalDateTime.now().minusMinutes(5)
        persistencePort.findRecoverable(threshold, 100).forEach { notification ->
            runCatching { deliveryService.redeliver(requireNotNull(notification.id)) }
                .onFailure { log.error("방치 알림 회수 실패 - notificationId={}", notification.id, it) }
        }
    }
}
