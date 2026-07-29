package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.friends.event.FriendRequestAccepted
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.application.dto.NotificationDeliveryCommand
import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import com.kdongsu5509.notifications.domain.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FriendNotificationEventListenerTest {
    @Mock
    private lateinit var deliveryService: NotificationDeliveryService

    private lateinit var listener: FriendNotificationEventListener

    @BeforeEach
    fun setUp() {
        listener = FriendNotificationEventListener(deliveryService)
    }

    @Test
    @DisplayName("친구 요청 이벤트를 수신 알림 발송 명령으로 번역한다")
    fun translate_friend_request_sent() {
        val event = FriendRequestSent(
            requesterEmail = "sender@example.com",
            requesterNickname = "보낸이",
            receiverEmail = "receiver@example.com",
            eventId = UUID.randomUUID(),
        )

        listener.handle(event)

        val command = argumentCaptor<NotificationDeliveryCommand>()
        verify(deliveryService).deliver(command.capture())
        assertThat(command.firstValue.eventId).isEqualTo(event.eventId)
        assertThat(command.firstValue.targetIdentifier).isEqualTo(event.receiverEmail)
        assertThat(command.firstValue.type).isEqualTo(NotificationType.FRIEND_REQUEST_RECEIVED)
    }

    @Test
    @DisplayName("친구 수락 이벤트를 요청자 대상 수락 알림으로 번역한다")
    fun translate_friend_request_accepted() {
        val event = FriendRequestAccepted(
            accepterEmail = "accepter@example.com",
            accepterNickname = "수락자",
            requesterEmail = "requester@example.com",
            eventId = UUID.randomUUID(),
        )

        listener.handle(event)

        val command = argumentCaptor<NotificationDeliveryCommand>()
        verify(deliveryService).deliver(command.capture())
        assertThat(command.firstValue.senderEmail).isEqualTo(event.accepterEmail)
        assertThat(command.firstValue.targetIdentifier).isEqualTo(event.requesterEmail)
        assertThat(command.firstValue.type).isEqualTo(NotificationType.FRIEND_REQUEST_ACCEPTED)
    }
}
