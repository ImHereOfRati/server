package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.common.testUtil.TestRabbitMQContainer
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.notifications.application.serivce.MessageIdempotencyService
import com.kdongsu5509.support.config.RabbitMQConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willDoNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.then
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest(
    classes = [
        FriendNotificationConsumer::class,
        RabbitMQConfig::class,
        RabbitAutoConfiguration::class
    ]
)
class FriendNotificationConsumerTest : TestRabbitMQContainer() {

    @MockitoBean
    private lateinit var notificationToUserCasePort: NotificationToUserCasePort

    @MockitoBean
    private lateinit var messageIdempotencyService: MessageIdempotencyService

    @Autowired
    private lateinit var consumer: FriendNotificationConsumer

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var amqpAdmin: AmqpAdmin

    @Test
    fun send_alert_good() {
        val receiverEmail = "test@example.com"
        val senderEmail = "sender@example.com"
        val body = "친구 요청이 왔습니다!"
        val messageId = UUID.randomUUID()

        val notificationMessageDto = NotificationMessageDto(
            type = NotificationType.FRIEND_REQUEST,
            receiverEmail = receiverEmail,
            senderEmail = senderEmail,
            message = body,
            data = null,
            timestamp = LocalDateTime.now(),
            messageId = messageId
        )

        given(messageIdempotencyService.isAlreadyProcessed(messageId.toString())).willReturn(false)
        willDoNothing().given(notificationToUserCasePort).send(
            senderNickname = senderEmail,
            senderEmail = senderEmail,
            receiverEmail = receiverEmail,
            type = NotificationType.FRIEND_REQUEST.name,
            body = body
        )

        consumer.receiveMessage(notificationMessageDto)

        then(notificationToUserCasePort).should(times(1)).send(
            senderNickname = senderEmail,
            senderEmail = senderEmail,
            receiverEmail = receiverEmail,
            type = NotificationType.FRIEND_REQUEST.name,
            body = body
        )
        then(messageIdempotencyService).should(times(1)).markAsProcessed(messageId.toString())
    }

    @Test
    fun send_alert_null_sender_email_throws_exception() {
        val messageId = UUID.randomUUID()
        val dto = NotificationMessageDto(
            type = NotificationType.FRIEND_REQUEST,
            receiverEmail = "test@example.com",
            senderEmail = null,
            message = "친구 요청이 왔습니다!",
            data = null,
            timestamp = LocalDateTime.now(),
            messageId = messageId
        )

        given(messageIdempotencyService.isAlreadyProcessed(messageId.toString())).willReturn(false)

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            consumer.receiveMessage(dto)
        }
    }

    @Test
    fun `이미 처리된 messageId는 중복 처리하지 않는다`() {
        val messageId = UUID.randomUUID()
        val dto = NotificationMessageDto(
            type = NotificationType.FRIEND_REQUEST,
            receiverEmail = "test@example.com",
            senderEmail = "sender@example.com",
            message = "친구 요청이 왔습니다!",
            data = null,
            timestamp = LocalDateTime.now(),
            messageId = messageId
        )

        given(messageIdempotencyService.isAlreadyProcessed(messageId.toString())).willReturn(true)

        consumer.receiveMessage(dto)

        then(notificationToUserCasePort).should(never()).send(any(), any(), any(), any(), any(), any())
        then(messageIdempotencyService).should(never()).markAsProcessed(any())
    }
}
