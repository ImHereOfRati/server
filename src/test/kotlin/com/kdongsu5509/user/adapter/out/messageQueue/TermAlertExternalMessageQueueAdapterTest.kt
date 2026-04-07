package com.kdongsu5509.user.adapter.out.messageQueue

import com.common.testUtil.TestRabbitMQContainer
import com.kdongsu5509.support.config.RabbitMQConfig
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.user.adapter.out.messageQueue.dto.NotificationType
import com.kdongsu5509.user.application.dto.AlertInformation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [
        TermAlertExternalMessageQueueAdapter::class,
        RabbitMQConfig::class,
        RabbitAutoConfiguration::class
    ]
)
class TermAlertExternalMessageQueueAdapterTest : TestRabbitMQContainer() {

    @Autowired
    private lateinit var adapter: TermAlertExternalMessageQueueAdapter

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var amqpAdmin: AmqpAdmin

    private val queueName = "noti.queue.service"

    @BeforeEach
    fun setUp() {
        val queue = Queue(queueName, true)

        val exchange = TopicExchange(RabbitMQConfig.EXCHANGE_NAME)

        val binding = BindingBuilder.bind(queue).to(exchange).with(RabbitMQConfig.SERVICE_ROUTING_KEY)

        amqpAdmin.declareQueue(queue)
        amqpAdmin.declareExchange(exchange)
        amqpAdmin.declareBinding(binding)
    }

    @Test
    fun send_alert_good() {
        val senderNickname = "rati"
        val receiverEmail = null
        val body = "gooooood"

        val alertInfo = AlertInformation(
            senderNickname = senderNickname,
            receiverEmail = receiverEmail,
            body = body
        )

        adapter.sendAlert(alertInfo)

        val receivedMessage = rabbitTemplate.receiveAndConvert(queueName) as? NotificationMessageDto

        assertThat(receivedMessage).isNotNull
        assertThat(receivedMessage?.senderEmail).isEqualTo(senderNickname)
        assertThat(receivedMessage?.receiverEmail).isEqualTo(receiverEmail)
        assertThat(receivedMessage?.type).isEqualTo(NotificationType.TERMS_UPDATE)
    }
}
