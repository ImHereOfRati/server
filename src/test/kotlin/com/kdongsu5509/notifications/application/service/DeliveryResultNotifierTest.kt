package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.shared.event.DomainEvent
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.LocalDateTime
import java.util.*

/**
 * 방어 대상은 수신증을 "보내지 않아야 하는" 경우다.
 * FCM 토큰이 없는 상대에게 수신증을 밀어넣으면 2차 발송 실패가 연쇄로 터진다.
 */
class DeliveryResultNotifierTest {
    private val eventPublisher: DomainEventPublisher = mock()

    private val notifier = DeliveryResultNotifier(eventPublisher, NoOpTransactionManager())

    private val requesterId = UUID.randomUUID()

    @Test
    @DisplayName("FCM 발송 성공은 요청자에게 수신증을 발행한다")
    fun fcm_success_publishes_receipt() {
        notifier.notifyDeliverySucceeded(notification(NotificationMethod.FCM, NotificationType.ARRIVAL), requesterId)

        val published = argumentCaptor<DomainEvent>()
        verify(eventPublisher).publish(published.capture())
        val receipt = published.firstValue as NotificationEvent
        assertThat(receipt.targetIdentifier).isEqualTo(requesterId.toString())
        assertThat(receipt.type).isEqualTo(NotificationType.DELIVERY_RESULT_NOTICE)
    }

    @Test
    @DisplayName("SMS 결과는 수신증을 발행하지 않는다")
    fun sms_result_publishes_no_receipt() {
        notifier.notifyDeliverySucceeded(notification(NotificationMethod.SMS, NotificationType.ARRIVAL), requesterId)

        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("수신증에 대한 수신증은 발행하지 않는다")
    fun meta_notification_publishes_no_receipt() {
        notifier.notifyDeliverySucceeded(
            notification(NotificationMethod.FCM, NotificationType.DELIVERY_RESULT_NOTICE),
            requesterId,
        )

        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("요청자를 알 수 없으면 수신증을 발행하지 않는다")
    fun unknown_requester_publishes_no_receipt() {
        notifier.notifyDeliverySucceeded(notification(NotificationMethod.FCM, NotificationType.ARRIVAL), requesterId = null)

        verifyNoInteractions(eventPublisher)
    }

    @Test
    @DisplayName("SMS 발송 실패는 운영 알림만 남기고 FCM 수신증은 발행하지 않는다")
    fun sms_failure_publishes_only_the_operations_alert() {
        notifier.notifyDeliveryFailed(
            notification(NotificationMethod.SMS, NotificationType.ARRIVAL),
            requesterId,
            IllegalStateException("SMS provider failure"),
        )

        val published = argumentCaptor<DomainEvent>()
        verify(eventPublisher).publish(published.capture())
        assertThat(published.firstValue).isInstanceOf(NotificationDeliveryFailed::class.java)
        verifyNoMoreInteractions(eventPublisher)
    }

    @Test
    @DisplayName("통지 발행이 실패해도 예외를 밖으로 내보내지 않는다")
    fun publish_failure_is_swallowed() {
        whenever(eventPublisher.publish(any())).thenThrow(IllegalStateException("publish failed"))

        notifier.notifyDeliverySucceeded(notification(NotificationMethod.FCM, NotificationType.ARRIVAL), requesterId)
    }

    private fun notification(method: NotificationMethod, type: NotificationType): Notification =
        Notification.reconstruct(
            id = 1L,
            dedupeKey = "dedupe",
            targetIdentifier = if (method == NotificationMethod.SMS) "01000000000" else UUID.randomUUID().toString(),
            method = method,
            senderAlias = "보낸이",
            type = type,
            title = "제목",
            body = "본문",
            extraData = emptyMap(),
            status = NotificationStatus.SENT,
            attempts = 0,
            lastError = null,
            sentAt = null,
            isRead = false,
            createdAt = LocalDateTime.now(),
        )

    private class NoOpTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
        override fun commit(status: TransactionStatus) = Unit
        override fun rollback(status: TransactionStatus) = Unit
    }
}
