package com.kdongsu5509.notifications.adapter.out.messageQueue

import com.kdongsu5509.notifications.application.dto.DlqQueueStats
import com.kdongsu5509.notifications.application.port.out.DlqManagementPort
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

/**
 * [DlqManagementPort]의 RabbitMQ 구현. Spring AMQP(`AmqpAdmin`/`RabbitTemplate`)와 브로커 Message 타입을 이 어댑터 안에 가둔다.
 */
@Component
class DlqManagementAdapter(
    private val amqpAdmin: AmqpAdmin,
    private val rabbitTemplate: RabbitTemplate
) : DlqManagementPort {

    override fun getQueueStats(queueName: String): DlqQueueStats? {
        val props = amqpAdmin.getQueueProperties(queueName) ?: return null
        return DlqQueueStats(
            messageCount = (props[RabbitAdmin.QUEUE_MESSAGE_COUNT] as? Number)?.toLong() ?: 0L,
            consumerCount = (props[RabbitAdmin.QUEUE_CONSUMER_COUNT] as? Number)?.toLong() ?: 0L
        )
    }

    override fun replayOne(queueName: String, targetExchange: String): Boolean {
        val message = rabbitTemplate.receive(queueName) ?: return false
        val originalRoutingKey = message.messageProperties.receivedRoutingKey ?: ""
        rabbitTemplate.send(targetExchange, originalRoutingKey, message)
        return true
    }

    override fun purge(queueName: String) {
        amqpAdmin.purgeQueue(queueName)
    }
}
