package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.support.external.AlertChannel
import com.kdongsu5509.support.external.ErrorAlertPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.*

@ExtendWith(MockitoExtension::class)
class NotificationEventListenerTest {
    @Mock
    private lateinit var deliveryService: NotificationDeliveryService

    @Mock
    private lateinit var errorAlertPort: ErrorAlertPort

    private lateinit var listener: NotificationEventListener

    @BeforeEach
    fun setUp() {
        listener = NotificationEventListener(deliveryService, errorAlertPort)
    }

    @Test
    @DisplayName("친구 요청 이벤트를 수신 알림 발송 명령으로 번역한다")
    fun translate_friend_request_sent() {
        val event = FriendRequestSent(
            requesterId = UUID.randomUUID(),
            requesterNickname = "보낸이",
            receiverId = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
        )

        listener.handle(event)

        val request = argumentCaptor<NotificationEvent>()
        verify(deliveryService).deliver(request.capture())
        assertThat(request.firstValue.eventId).isEqualTo(event.eventId)
        assertThat(request.firstValue.targetIdentifier).isEqualTo(event.receiverId.toString())
        assertThat(request.firstValue.type).isEqualTo(NotificationType.FRIEND_REQUEST_RECEIVED)
    }

    @Test
    @DisplayName("친구 수락 이벤트를 요청자 대상 수락 알림으로 번역한다")
    fun translate_friend_request_accepted() {
        val event = FriendRequestAccepted(
            accepterId = UUID.randomUUID(),
            accepterNickname = "수락자",
            requesterId = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
        )

        listener.handle(event)

        val request = argumentCaptor<NotificationEvent>()
        verify(deliveryService).deliver(request.capture())
        assertThat(request.firstValue.senderId).isEqualTo(event.accepterId)
        assertThat(request.firstValue.targetIdentifier).isEqualTo(event.requesterId.toString())
        assertThat(request.firstValue.type).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPTED)
    }

    @Test
    @DisplayName("발송 실패 이벤트를 서버 오류 채널 경보로 옮긴다")
    fun translate_delivery_failure_to_alert() {
        val event = NotificationDeliveryFailed(
            notificationId = 7L,
            targetIdentifier = UUID.randomUUID().toString(),
            notificationType = NotificationType.FRIEND_REQUEST_RECEIVED.name,
            errorType = "IllegalStateException",
            errorMessage = "boom",
        )

        listener.handle(event)

        val alert = argumentCaptor<com.kdongsu5509.support.external.AlertMessage>()
        verify(errorAlertPort).send(org.mockito.kotlin.eq(AlertChannel.SERVER_ERROR), alert.capture())
        assertThat(alert.firstValue.content).contains("7", "IllegalStateException", "boom")
    }
}
