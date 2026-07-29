package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationRequested
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationReceiptPublisher(
    private val eventPublisher: DomainEventPublisher,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun publish(notification: Notification, type: NotificationType) {
        if (notification.type.isMeta) return
        eventPublisher.publish(NotificationRequested.deliveryReceipt(notification.senderEmail, type))
    }
}
