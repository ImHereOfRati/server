package com.kdongsu5509.user.adapter.out.messageQueue

import com.common.testUtil.TestRabbitMQContainer
import com.kdongsu5509.support.config.RabbitMQConfig
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationType
import com.kdongsu5509.user.application.dto.AlertResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        FriendAlertExternalMessageQueueAdapter::class,
        RabbitMQConfig::class,
        RabbitAutoConfiguration::class
    ]
)
class FriendAlertExternalMessageQueueAdapterTest : TestRabbitMQContainer() {

    @Autowired
    private lateinit var adapter: FriendAlertExternalMessageQueueAdapter

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    private val queueName = RabbitMQConfig.FRIEND_QUEUE

    @Test
    fun send_alert_good() {
        val senderNickname = "rati"
        val receiverEmail = "test@example.com"
        val body = "ì¹œêµ¬ ?”ì²­???”ìŠµ?ˆë‹¤!"

        val alertInfo = AlertResponse(
            senderNickname = senderNickname,
            receiverEmail = receiverEmail,
            body = body
        )

        adapter.sendAlert(alertInfo)

        val receivedMessage = rabbitTemplate.receiveAndConvert(queueName) as? NotificationMessageDto

        assertThat(receivedMessage).isNotNull
        assertThat(receivedMessage?.senderEmail).isEqualTo(senderNickname)
        assertThat(receivedMessage?.receiverEmail).isEqualTo(receiverEmail)
        assertThat(receivedMessage?.type).isEqualTo(NotificationType.FRIEND_REQUEST)
    }
}
