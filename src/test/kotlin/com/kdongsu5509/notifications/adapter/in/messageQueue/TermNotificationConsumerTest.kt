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
        TermNotificationConsumer::class,
        RabbitMQConfig::class,
        RabbitAutoConfiguration::class
    ]
)
class TermNotificationConsumerTest : TestRabbitMQContainer() {

    @MockitoBean
    private lateinit var notificationToUserCasePort: NotificationToUserCasePort

    @Autowired
    private lateinit var consumer: TermNotificationConsumer

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var amqpAdmin: AmqpAdmin

    @Test
    fun send_alert_good() {
        val receiverEmail = "test@example.com"
        val senderEmail = null
        val body = "약관 업데이트 도미 "

        val notificationMessageDto = NotificationMessageDto(
            type = NotificationType.TERMS_UPDATE,
            receiverEmail = receiverEmail,
            senderEmail = senderEmail,
            message = body,
            data = null,
            timestamp = LocalDateTime.now()
        )

        val expectedSenderEmail = senderEmail ?: "IMHERE_SERVICE"
        willDoNothing().given(notificationToUserCasePort).send(
            senderNickname = "System",
            senderEmail = expectedSenderEmail,
            receiverEmail = receiverEmail,
            type = NotificationType.TERMS_UPDATE.name,
            body = body
        )

        consumer.receiveMessage(notificationMessageDto)

        then(notificationToUserCasePort).should(times(1)).send(
            senderNickname = "System",
            senderEmail = senderEmail ?: "IMHERE_SERVICE",
            receiverEmail = receiverEmail,
            type = NotificationType.TERMS_UPDATE.name,
            body = body
        )
    }

}
