package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@Component
class DeliveryResultNotifier(
    private val eventPublisher: DomainEventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun notifyDeliverySucceeded(notification: Notification, requesterId: UUID?) {
        val receiptRecipient = receiptRecipientOrNull(notification, requesterId) ?: return

        try {
            newTransaction.execute {
                eventPublisher.publish(
                    NotificationEvent.deliveryReceipt(receiptRecipient, NotificationType.DELIVERY_RESULT_NOTICE)
                )
            }
        } catch (notifyError: Exception) {
            log.error("알림 발송 성공 통지 중 오류", notifyError)
        }
    }

    fun notifyDeliveryFailed(notification: Notification, requesterId: UUID?, error: Throwable) {
        val receiptRecipient = receiptRecipientOrNull(notification, requesterId)

        try {
            newTransaction.execute {
                eventPublisher.publish(
                    NotificationDeliveryFailed(
                        notificationId = notification.id,
                        targetIdentifier = notification.targetIdentifier,
                        notificationType = notification.type.name,
                        errorType = error.javaClass.simpleName,
                        errorMessage = error.message,
                    )
                )
                if (receiptRecipient != null) {
                    eventPublisher.publish(
                        NotificationEvent.deliveryReceipt(receiptRecipient, NotificationType.DELIVERY_FAILED_NOTICE)
                    )
                }
            }
        } catch (notifyError: Exception) {
            log.error("알림 발송 실패 통지 중 오류", notifyError)
        }
    }

    /** 수신증을 받을 사람. 보내면 안 되는 경우 null. */
    private fun receiptRecipientOrNull(notification: Notification, requesterId: UUID?): UUID? {
        if (notification.method != NotificationMethod.FCM) return null
        if (notification.type.isMeta) return null
        return requesterId
    }
}
