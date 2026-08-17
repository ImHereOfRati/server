package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.RetryableFcmException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDateTime
import java.util.*

/**
 * 파사드가 방어하는 것은 두 가지다.
 *  - 멱등성: 같은 이벤트로 외부 채널을 두 번 부르지 않는다.
 *  - 조율 순서: 발송 결과에 맞는 상태를 기록하고, 통지를 적절한 시점에 한 번만 한다.
 *
 * 각 단계의 내부 동작은 협력자 테스트가 맡는다. 여기서는 협력자를 전부 목으로 세운다.
 * 스프링 컨텍스트를 띄우는 이유는 @Retryable/@Recover 경계가 검증 대상이기 때문이다.
 */
@SpringJUnitConfig(NotificationDeliveryFacadeTest.Config::class)
class NotificationDeliveryFacadeTest @Autowired constructor(
    private val facade: NotificationDeliveryFacade,
    private val registrar: NotificationRegister,
    private val channelSender: NotificationChannelSender,
    private val resultNotifier: DeliveryResultNotifier,
) {
    @Configuration
    @EnableRetry
    class Config {
        @Bean
        fun registrar(): NotificationRegister = mock()

        @Bean
        fun channelSender(): NotificationChannelSender = mock()

        @Bean
        fun resultNotifier(): DeliveryResultNotifier = mock()

        @Bean
        fun facade(
            registrar: NotificationRegister,
            channelSender: NotificationChannelSender,
            resultNotifier: DeliveryResultNotifier,
        ) = NotificationDeliveryFacade(registrar, channelSender, resultNotifier)
    }

    private val request = NotificationEvent(
        eventId = UUID.randomUUID(),
        senderNickname = "보낸이",
        senderId = UUID.randomUUID(),
        notificationMethod = NotificationMethod.FCM,
        targetIdentifier = UUID.randomUUID().toString(),
        type = NotificationType.FRIEND_REQUEST_RECEIVED,
    )

    private val dedupeKey get() = Notification.dedupeKeyOf(request.eventId, request.notificationMethod)

    @BeforeEach
    fun setUp() {
        reset(registrar, channelSender, resultNotifier)
        whenever(registrar.markAsSent(any())).thenReturn(notification(NotificationStatus.SENT))
        whenever(registrar.markFailed(any(), any())).thenReturn(notification(NotificationStatus.FAILED))
    }

    // --- 멱등성 ---------------------------------------------------------------

    @Test
    @DisplayName("이미 발송에 성공한 이벤트는 외부 채널을 다시 호출하지 않는다")
    fun already_sent_event_is_skipped() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(notification(NotificationStatus.SENT))

        facade.deliver(request)

        verify(channelSender, never()).sendViaExternalMethod(any())
        verify(registrar, never()).register(any())
    }

    @Test
    @DisplayName("최종 실패한 이벤트는 다시 접수하지 않고 그대로 재발송한다")
    fun failed_event_is_redelivered_without_new_reservation() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(notification(NotificationStatus.FAILED))

        facade.deliver(request)

        verify(channelSender).sendViaExternalMethod(any())
        verify(registrar, never()).register(any())
    }

    @Test
    @DisplayName("접수 직전 다른 실행이 같은 이벤트를 선점하면 발송하지 않는다")
    fun losing_the_reservation_race_skips_delivery() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(null)
        whenever(registrar.register(request)).thenThrow(DataIntegrityViolationException("uk_notification_dedupe_key"))

        facade.deliver(request)

        verify(channelSender, never()).sendViaExternalMethod(any())
        verifyNoInteractions(resultNotifier)
    }

    // --- 조율 순서 -------------------------------------------------------------

    @Test
    @DisplayName("발송에 성공하면 SENT로 기록하고 성공 통지를 한다")
    fun success_marks_sent_then_notifies() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(notification(NotificationStatus.FAILED))

        facade.deliver(request)

        inOrder(channelSender, registrar, resultNotifier) {
            verify(channelSender).sendViaExternalMethod(any())
            verify(registrar).markAsSent(NOTIFICATION_ID)
            verify(resultNotifier).notifyDeliverySucceeded(any(), eq(request.senderId))
        }
        verify(resultNotifier, never()).notifyDeliveryFailed(any(), anyOrNull(), any())
    }

    @Test
    @DisplayName("재시도 대상이 아닌 실패는 FAILED로 기록하고 즉시 실패 통지 후 예외를 전파한다")
    fun non_retryable_failure_notifies_immediately() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(notification(NotificationStatus.FAILED))
        whenever(channelSender.sendViaExternalMethod(any())).thenThrow(IllegalStateException("토큰 없음"))

        // 재시도 대상이 아닌 예외는 감싸지 않고 원래 타입 그대로 올라와야 한다.
        assertThatThrownBy { facade.deliver(request) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("토큰 없음")

        verify(registrar).markFailed(NOTIFICATION_ID, "토큰 없음")
        verify(resultNotifier).notifyDeliveryFailed(any(), eq(request.senderId), any())
        verify(channelSender, times(1)).sendViaExternalMethod(any())
    }

    @Test
    @DisplayName("재시도 대상 실패는 소진되기 전까지 통지하지 않고, 소진 후 한 번만 통지한다")
    fun retryable_failure_notifies_once_after_retries_are_exhausted() {
        whenever(registrar.findByDedupeKey(dedupeKey)).thenReturn(notification(NotificationStatus.FAILED))
        whenever(channelSender.sendViaExternalMethod(any())).thenThrow(RetryableFcmException("일시적 오류"))

        facade.deliver(request)

        verify(channelSender, times(3)).sendViaExternalMethod(any())
        verify(registrar, times(3)).markFailed(eq(NOTIFICATION_ID), any())
        verify(resultNotifier, times(1)).notifyDeliveryFailed(any(), eq(request.senderId), any())
    }

    private fun notification(status: NotificationStatus): Notification =
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
            attempts = 0,
            lastError = null,
            sentAt = null,
            isRead = false,
            createdAt = LocalDateTime.now(),
        )

    private companion object {
        const val NOTIFICATION_ID = 1L
    }
}
