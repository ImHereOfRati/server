package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.application.dto.NotificationDeliveryCommand
import com.kdongsu5509.notifications.application.service.channel.NotificationDeliveryChannel
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID

@SpringJUnitConfig(NotificationDeliveryServiceTest.Config::class)
class NotificationDeliveryServiceTest @Autowired constructor(
    private val service: NotificationDeliveryService,
    private val channel: NotificationDeliveryChannel,
    private val recorder: NotificationRecorder,
    private val failureNotifier: NotificationFailureNotifier,
) {
    @Configuration
    @EnableRetry
    class Config {
        @Bean
        fun channel(): NotificationDeliveryChannel = mock {
            on { method }.thenReturn(NotificationMethod.FCM)
        }

        @Bean
        fun recorder(): NotificationRecorder = mock()

        @Bean
        fun failureNotifier(): NotificationFailureNotifier = mock()

        @Bean
        fun receiptPublisher(): NotificationReceiptPublisher = mock()

        @Bean
        fun service(
            channel: NotificationDeliveryChannel,
            recorder: NotificationRecorder,
            failureNotifier: NotificationFailureNotifier,
            receiptPublisher: NotificationReceiptPublisher,
        ) = NotificationDeliveryService(listOf(channel), recorder, failureNotifier, receiptPublisher)
    }

    private val command = NotificationDeliveryCommand(
        eventId = UUID.randomUUID(),
        senderNickname = "보낸이",
        senderEmail = "sender@example.com",
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = "receiver@example.com",
        type = NotificationType.DELIVERY_FAILED_NOTICE,
    )

    @BeforeEach
    fun setUp() {
        reset(channel, recorder, failureNotifier)
    }

    @Test
    @DisplayName("이미 예약된 이벤트는 외부 채널을 다시 호출하지 않는다")
    fun duplicate_reservation_is_skipped() {
        whenever(recorder.reserve(command)).thenReturn(null)

        service.deliver(command)

        verify(channel, never()).send(any())
    }

    @Test
    @DisplayName("일시적 FCM 오류를 세 번 재시도하고 DEAD 전이 후 실패 통보한다")
    fun retryable_failure_retries_three_times_and_recovers() {
        val pending = notification()
        val failed1 = pending.markFailed("1")
        val failed2 = failed1.markFailed("2")
        val dead = failed2.markFailed("3")
        val error = RetryableFcmException("temporary", RuntimeException("firebase"))

        whenever(recorder.reserve(command)).thenReturn(pending, failed1, failed2)
        whenever(recorder.markFailed(1L, error)).thenReturn(failed1, failed2, dead)
        whenever(recorder.findByDedupeKey(pending.dedupeKey)).thenReturn(dead)
        whenever(channel.send(any())).thenThrow(error)

        service.deliver(command)

        verify(channel, times(3)).send(any())
        verify(recorder, times(3)).markFailed(1L, error)
        verify(failureNotifier).notifyFailure(dead, error)
    }

    private fun notification(): Notification =
        Notification.reconstruct(
            id = 1L,
            dedupeKey = Notification.dedupeKeyOf(command.eventId, command.notificationMethod),
            targetIdentifier = command.targetIdentifier,
            method = command.notificationMethod,
            senderEmail = command.senderEmail,
            senderNickname = command.senderNickname,
            type = command.type,
            title = "제목",
            body = "본문",
            path = "/record/send-history",
            extraData = emptyMap(),
            status = com.kdongsu5509.notifications.domain.NotificationStatus.PENDING,
            attempts = 0,
            lastError = null,
            sentAt = null,
            isRead = false,
            createdAt = java.time.LocalDateTime.now(),
        )
}
