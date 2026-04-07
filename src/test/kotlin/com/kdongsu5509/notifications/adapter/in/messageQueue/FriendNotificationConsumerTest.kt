package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.common.testUtil.TestRabbitMQContainer
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.willDoNothing
import org.mockito.Mockito.times
import org.mockito.kotlin.then
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime

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

        val notificationMessageDto = NotificationMessageDto(
            type = NotificationType.FRIEND_REQUEST,
            receiverEmail = receiverEmail,
            senderEmail = senderEmail,
            message = body,
            data = null,
            timestamp = LocalDateTime.now()
        )

        willDoNothing().given(notificationToUserCasePort).send(
            receiverEmail,
            type = NotificationType.FRIEND_REQUEST.name,
            body = body
        )

        consumer.receiveMessage(notificationMessageDto)

        then(notificationToUserCasePort).should(times(1)).send(
            receiverEmail, NotificationType.FRIEND_REQUEST.name, body
        )
    }

}
