package com.kdongsu5509.notifications.adapter.out.messageQueue

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.support.config.RabbitMQConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.amqp.rabbit.core.RabbitTemplate

@ExtendWith(MockitoExtension::class)
class NotificationRabbitMQProducerAdapterTest {

    @Mock
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var adapter: NotificationRabbitMQProducerAdapter

    @BeforeEach
    fun setUp() {
        adapter = NotificationRabbitMQProducerAdapter(rabbitTemplate)
    }

    @Test
    @DisplayName("알림 커맨드를 받아 올바른 라우팅 키로 메시지를 발행한다")
    fun send_success() {
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifier = "target@example.com",
            type = "FRIEND_REQUEST_RECEIVED",
            extraData = mapOf("key" to "value")
        )

        adapter.send(command)

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_NAME),
            eq(RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED),
            argThat<NotificationMessageDto> { msg ->
                msg.category == NotificationType.FRIEND_REQUEST_RECEIVED &&
                        msg.sender.email == "sender@example.com" &&
                        msg.sender.nickname == "sender" &&
                        msg.receiver.email == "target@example.com" &&
                        msg.data!!["key"] == "value"
            }
        )
    }
}
