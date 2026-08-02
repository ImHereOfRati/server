package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.application.port.out.SenderAliasPort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.LocalDateTime
import java.util.*

@SpringJUnitConfig(NotificationDeliveryServiceTest.Config::class)
class NotificationDeliveryServiceTest @Autowired constructor(
    private val service: NotificationDeliveryService,
    private val persistencePort: NotificationPersistencePort,
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val eventPublisher: DomainEventPublisher,
) {
    @Configuration
    @EnableRetry
    class Config {
        @Bean
        fun persistencePort(): NotificationPersistencePort = mock()

        @Bean
        fun senderAliasPort(): SenderAliasPort = mock()

        @Bean
        fun firebasePort(): FirebasePort = mock()

        @Bean
        fun fcmTokenPersistencePort(): FcmTokenPersistencePort = mock()

        @Bean
        fun externalMessagePort(): ExternalMessagePort = mock()

        @Bean
        fun eventPublisher(): DomainEventPublisher = mock()

        @Bean
        fun transactionManager(): PlatformTransactionManager = object : PlatformTransactionManager {
            override fun getTransaction(definition: TransactionDefinition?): TransactionStatus =
                SimpleTransactionStatus()

            override fun commit(status: TransactionStatus) = Unit
            override fun rollback(status: TransactionStatus) = Unit
        }

        @Bean
        fun service(
            persistencePort: NotificationPersistencePort,
            senderAliasPort: SenderAliasPort,
            firebasePort: FirebasePort,
            fcmTokenPersistencePort: FcmTokenPersistencePort,
            externalMessagePort: ExternalMessagePort,
            eventPublisher: DomainEventPublisher,
            transactionManager: PlatformTransactionManager,
        ) = NotificationDeliveryService(
            persistencePort,
            senderAliasPort,
            firebasePort,
            fcmTokenPersistencePort,
            externalMessagePort,
            eventPublisher,
            transactionManager,
        )
    }

    private val receiverId: UUID = UUID.randomUUID()

    private val request = NotificationEvent(
        eventId = UUID.randomUUID(),
        senderNickname = "보낸이",
        senderId = UUID.randomUUID(),
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = receiverId.toString(),
        type = NotificationType.FRIEND_REQUEST_RECEIVED,
    )

    private val dedupeKey get() = Notification.dedupeKeyOf(request.eventId, request.notificationMethod)

    @BeforeEach
    fun setUp() {
        reset(persistencePort, firebasePort, fcmTokenPersistencePort, eventPublisher)
        // 저장은 넘긴 값을 그대로 돌려준다. 상태 전이 결과를 그대로 관찰하기 위함이다.
        whenever(persistencePort.save(any())).thenAnswer { it.arguments[0] as Notification }
        whenever(fcmTokenPersistencePort.findByOwnerId(receiverId))
            .thenReturn(FcmToken(ownerId = receiverId, fcmToken = "token", deviceType = DeviceType.AOS))
    }

    @Test
    @DisplayName("이미 발송에 성공한 이벤트는 외부 채널을 다시 호출하지 않는다")
    fun duplicate_reservation_is_skipped() {
        whenever(persistencePort.findByDedupeKey(dedupeKey))
            .thenReturn(notification(NotificationStatus.SENT, attempts = 0))

        service.deliver(request)

        verify(firebasePort, never()).send(any(), any(), any())
        verify(persistencePort, never()).save(any())
    }

    @Test
    @DisplayName("일시적 FCM 오류를 세 번 재시도하고 마지막에 DEAD로 저장한다")
    fun retryable_failure_retries_three_times_and_goes_dead() {
        val error = RetryableFcmException("temporary", RuntimeException("firebase"))

        // 재시도마다 다시 접수되므로, 그때그때의 누적 시도 횟수를 돌려준다.
        whenever(persistencePort.findByDedupeKey(dedupeKey)).thenReturn(
            notification(NotificationStatus.FAILED, attempts = 0),
            notification(NotificationStatus.FAILED, attempts = 1),
            notification(NotificationStatus.FAILED, attempts = 2),
            notification(NotificationStatus.DEAD, attempts = 3),
        )
        whenever(persistencePort.findById(NOTIFICATION_ID)).thenReturn(
            notification(NotificationStatus.FAILED, attempts = 0),
            notification(NotificationStatus.FAILED, attempts = 1),
            notification(NotificationStatus.FAILED, attempts = 2),
        )
        whenever(firebasePort.send(any(), any(), any())).thenThrow(error)

        service.deliver(request)

        verify(firebasePort, times(3)).send(any(), any(), any())

        val saved = argumentCaptor<Notification>()
        verify(persistencePort, times(3)).save(saved.capture())
        assertThat(saved.allValues.map { it.status }).containsExactly(
            NotificationStatus.FAILED,
            NotificationStatus.FAILED,
            NotificationStatus.DEAD,
        )
    }

    @Test
    @DisplayName("발송에 성공하면 SENT로 저장하고 요청자에게 결과 수신증을 발행한다")
    fun success_marks_sent_and_publishes_receipt() {
        whenever(persistencePort.findByDedupeKey(dedupeKey))
            .thenReturn(notification(NotificationStatus.FAILED, attempts = 0))
        whenever(persistencePort.findById(NOTIFICATION_ID))
            .thenReturn(notification(NotificationStatus.FAILED, attempts = 0))

        service.deliver(request)

        val saved = argumentCaptor<Notification>()
        verify(persistencePort).save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(NotificationStatus.SENT)

        val receipt = argumentCaptor<NotificationEvent>()
        verify(eventPublisher).publish(receipt.capture())
        assertThat(receipt.firstValue.targetIdentifier).isEqualTo(request.senderId.toString())
        assertThat(receipt.firstValue.type).isEqualTo(NotificationType.DELIVERY_RESULT_NOTICE)
    }

    @Test
    @DisplayName("결과 수신증 발행이 실패해도 이미 성공한 발송을 실패로 되돌리지 않는다")
    fun receipt_failure_does_not_change_delivery_result() {
        whenever(persistencePort.findByDedupeKey(dedupeKey))
            .thenReturn(notification(NotificationStatus.FAILED, attempts = 0))
        whenever(persistencePort.findById(NOTIFICATION_ID))
            .thenReturn(notification(NotificationStatus.FAILED, attempts = 0))
        whenever(eventPublisher.publish(any())).thenThrow(IllegalStateException("receipt failed"))

        service.deliver(request)

        val saved = argumentCaptor<Notification>()
        verify(persistencePort).save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(NotificationStatus.SENT)
    }

    private fun notification(status: NotificationStatus, attempts: Int): Notification =
        Notification.reconstruct(
            id = NOTIFICATION_ID,
            dedupeKey = dedupeKey,
            targetIdentifier = request.targetIdentifier,
            method = request.notificationMethod,
            senderAlias = request.senderNickname,
            type = request.type,
            title = "제목",
            body = "본문",
            extraData = emptyMap(),
            status = status,
            attempts = attempts,
            lastError = null,
            sentAt = null,
            isRead = false,
            createdAt = LocalDateTime.now(),
        )

    private companion object {
        const val NOTIFICATION_ID = 1L
    }
}
