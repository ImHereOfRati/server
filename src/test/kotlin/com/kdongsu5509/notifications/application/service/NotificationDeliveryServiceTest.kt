package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationRequested
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
    private val sender: NotificationSender,
    private val recorder: NotificationRecorder,
    private val outcomeNotifier: NotificationOutcomeNotifier,
) {
    @Configuration
    @EnableRetry
    class Config {
        @Bean
        fun sender(): NotificationSender = mock()

        @Bean
        fun recorder(): NotificationRecorder = mock()

        @Bean
        fun outcomeNotifier(): NotificationOutcomeNotifier = mock()

        @Bean
        fun service(
            recorder: NotificationRecorder,
            sender: NotificationSender,
            outcomeNotifier: NotificationOutcomeNotifier,
        ) = NotificationDeliveryService(recorder, sender, outcomeNotifier)
    }

    private val request = NotificationRequested(
        eventId = UUID.randomUUID(),
        senderNickname = "보낸이",
        senderEmail = "sender@example.com",
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = "receiver@example.com",
        type = NotificationType.DELIVERY_FAILED_NOTICE,
    )

    @BeforeEach
    fun setUp() {
        reset(sender, recorder, outcomeNotifier)
    }

    @Test
    @DisplayName("이미 예약된 이벤트는 외부 채널을 다시 호출하지 않는다")
    fun duplicate_reservation_is_skipped() {
        whenever(recorder.reserve(request)).thenReturn(null)

        service.deliver(request)

        verify(sender, never()).send(any())
    }

    @Test
    @DisplayName("일시적 FCM 오류를 세 번 재시도하고 DEAD 전이 후 실패 통보한다")
    fun retryable_failure_retries_three_times_and_recovers() {
        val pending = notification()
        val failed1 = pending.markFailed("1")
        val failed2 = failed1.markFailed("2")
        val dead = failed2.markFailed("3")
        val error = RetryableFcmException("temporary", RuntimeException("firebase"))

        whenever(recorder.reserve(request)).thenReturn(pending, failed1, failed2)
        whenever(recorder.markFailed(1L, error)).thenReturn(failed1, failed2, dead)
        whenever(recorder.findByDedupeKey(pending.dedupeKey)).thenReturn(dead)
        whenever(sender.send(any())).thenThrow(error)

        service.deliver(request)

        verify(sender, times(3)).send(any())
        verify(recorder, times(3)).markFailed(1L, error)
        verify(outcomeNotifier).failure(dead, error)
    }

    @Test
    @DisplayName("발송 결과 통지 실패는 이미 성공한 알림을 실패 상태로 바꾸지 않는다")
    fun outcome_failure_does_not_change_delivery_result() {
        val pending = notification()
        whenever(recorder.reserve(request)).thenReturn(pending)
        whenever(recorder.markSent(1L)).thenReturn(pending.markSent(java.time.LocalDateTime.now()))
        whenever(outcomeNotifier.success(pending)).thenThrow(IllegalStateException("receipt failed"))

        service.deliver(request)

        verify(recorder, never()).markFailed(any(), any())
    }

    private fun notification(): Notification =
        Notification.reconstruct(
            id = 1L,
            dedupeKey = Notification.dedupeKeyOf(request.eventId, request.notificationMethod),
            targetIdentifier = request.targetIdentifier,
            method = request.notificationMethod,
            senderEmail = request.senderEmail,
            senderNickname = request.senderNickname,
            type = request.type,
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
