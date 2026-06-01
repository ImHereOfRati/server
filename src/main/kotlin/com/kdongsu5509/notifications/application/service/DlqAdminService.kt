package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqQueueInfoResponse
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqReplayResponse
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class DlqAdminService(
    private val amqpAdmin: AmqpAdmin,
    private val rabbitTemplate: RabbitTemplate
) {
    companion object {
        /** DLQ → 재발행 exchange 매핑 */
        val DLQ_REPLAY_TARGET: Map<String, String> = mapOf(
            RabbitMQConfig.FRIEND_DLQ to RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.SERVICE_DLQ to RabbitMQConfig.EXCHANGE_NAME
        )
    }

    fun getAllDlqInfo(): List<DlqQueueInfoResponse> =
        DLQ_REPLAY_TARGET.keys.map { getQueueInfo(it) }

    fun getQueueInfo(queueName: String): DlqQueueInfoResponse {
        requireKnownDlq(queueName)
        val props = amqpAdmin.getQueueProperties(queueName)
            ?: error("DLQ 큐를 찾을 수 없습니다: $queueName")
        return DlqQueueInfoResponse(
            queueName = queueName,
            messageCount = (props[RabbitAdmin.QUEUE_MESSAGE_COUNT] as? Number)?.toLong() ?: 0L,
            consumerCount = (props[RabbitAdmin.QUEUE_CONSUMER_COUNT] as? Number)?.toLong() ?: 0L
        )
    }

    /**
     * DLQ에서 최대 [count]개의 메시지를 꺼내 원본 Exchange로 재발행한다.
     * [count]가 null이면 DLQ의 모든 메시지를 재발행한다.
     * 재발행된 메시지는 DLQ에서 소비(제거)된다.
     */
    fun replayMessages(queueName: String, count: Int? = null): DlqReplayResponse {
        requireKnownDlq(queueName)
        val exchange = DLQ_REPLAY_TARGET.getValue(queueName)
        val limit = count ?: Int.MAX_VALUE

        var replayed = 0
        repeat(limit) {
            val message = rabbitTemplate.receive(queueName) ?: return DlqReplayResponse(queueName, replayed)
            val originalRoutingKey = message.messageProperties.receivedRoutingKey ?: ""
            rabbitTemplate.send(exchange, originalRoutingKey, message)
            replayed++
        }
        return DlqReplayResponse(queueName, replayed)
    }

    /**
     * DLQ의 모든 메시지를 삭제한다.
     */
    fun purgeQueue(queueName: String) {
        requireKnownDlq(queueName)
        amqpAdmin.purgeQueue(queueName)
    }

    private fun requireKnownDlq(queueName: String) {
        require(DLQ_REPLAY_TARGET.containsKey(queueName)) {
            "알 수 없는 DLQ입니다: $queueName. 허용 목록: ${DLQ_REPLAY_TARGET.keys}"
        }
    }
}
