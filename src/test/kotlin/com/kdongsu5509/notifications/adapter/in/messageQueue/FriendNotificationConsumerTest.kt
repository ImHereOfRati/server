package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.notifications.application.service.MessageIdempotencyService
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import com.kdongsu5509.support.external.DiscordMessageSendPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*
import org.assertj.core.api.Assertions.assertThatThrownBy

@ExtendWith(MockitoExtension::class)
class FriendNotificationConsumerTest {

    @Mock
    private lateinit var notificationDispatcherUseCase: NotificationDispatcherUseCase

    @Mock
    private lateinit var messageIdempotencyService: MessageIdempotencyService

    @Mock
    private lateinit var notificationEnqueueUseCase: NotificationEnqueueUseCase

    @Mock
    private lateinit var discordMessageSendPort: DiscordMessageSendPort

    private lateinit var consumer: FriendNotificationConsumer

    @BeforeEach
    fun setUp() {
        val failureNotifier = ConsumerFailureNotifier(notificationEnqueueUseCase, discordMessageSendPort)
        consumer = FriendNotificationConsumer(
            notificationDispatcherUseCase,
            messageIdempotencyService,
            notificationEnqueueUseCase,
            failureNotifier
        )
    }

    private fun createDto(): NotificationMessageDto {
        return NotificationMessageDto(
            category = NotificationType.FRIEND_REQUEST_RECEIVED,
            sender = NotificationPersonInfo("sender@test.com", "senderNick"),
            receiver = NotificationPersonInfo("receiver@test.com", "receiverNick"),
            data = mapOf("key" to "value"),
            timestamp = LocalDateTime.now(),
            messageId = UUID.randomUUID()
        )
    }

    @Test
    @DisplayName("중복 메시지인 경우 처리를 생략한다")
    fun receiveMessage_duplicate() {
        val dto = createDto()
        whenever(messageIdempotencyService.isAlreadyProcessed(dto.messageId.toString())).thenReturn(true)

        consumer.receiveMessage(dto)

        verify(notificationDispatcherUseCase, never()).dispatch(any())
        verify(messageIdempotencyService, never()).markAsProcessed(any())
    }

    @Test
    @DisplayName("정상 메시지인 경우 dispatch 후 처리 완료로 마킹한다")
    fun receiveMessage_success() {
        val dto = createDto()
        whenever(messageIdempotencyService.isAlreadyProcessed(dto.messageId.toString())).thenReturn(false)

        consumer.receiveMessage(dto)

        verify(notificationDispatcherUseCase).dispatch(any<NotificationCommand>())
        verify(messageIdempotencyService).markAsProcessed(dto.messageId.toString())
    }

    @Test
    @DisplayName("dispatch 시 예외가 발생하면 markAsProcessed를 호출하지 않고 예외를 전파한다")
    fun receiveMessage_exception() {
        val dto = createDto()
        whenever(messageIdempotencyService.isAlreadyProcessed(dto.messageId.toString())).thenReturn(false)
        doThrow(RuntimeException("Dispatch error")).whenever(notificationDispatcherUseCase).dispatch(any<NotificationCommand>())

        assertThatThrownBy { consumer.receiveMessage(dto) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessage("Dispatch error")

        verify(messageIdempotencyService, never()).markAsProcessed(dto.messageId.toString())
    }
}
