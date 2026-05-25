package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.out.NotificationProducePort
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class NotificationEnqueueServiceTest {

    @Mock
    private lateinit var notificationProducePort: NotificationProducePort

    private lateinit var service: NotificationEnqueueService

    @BeforeEach
    fun setUp() {
        service = NotificationEnqueueService(notificationProducePort)
    }

    @Test
    @DisplayName("단건 알림 커맨드를 포트로 전달한다")
    fun enqueue_success() {
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifier = "target@example.com",
            type = "FRIEND_REQUEST_RECEIVED"
        )

        service.enqueue(command)

        verify(notificationProducePort).send(command)
    }

    @Test
    @DisplayName("다건 알림 커맨드를 각각 단건으로 분리하여 포트로 전달한다")
    fun enqueueMultiple_success() {
        val command = MultipleNotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifiers = listOf("target1@example.com", "target2@example.com"),
            type = "FRIEND_REQUEST_RECEIVED"
        )

        service.enqueueMultiple(command)

        val captor = argumentCaptor<NotificationCommand>()
        verify(notificationProducePort, times(2)).send(captor.capture())
        val capturedCommands = captor.allValues

        assertThat(capturedCommands).hasSize(2)
        assertThat(capturedCommands.map { it.targetIdentifier })
            .containsExactlyInAnyOrder("target1@example.com", "target2@example.com")
        assertThat(capturedCommands).allSatisfy { cmd ->
            assertThat(cmd.senderNickname).isEqualTo("sender")
            assertThat(cmd.type).isEqualTo("FRIEND_REQUEST_RECEIVED")
        }
    }
}
