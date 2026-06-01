package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.support.config.RabbitMQConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class DlqAdminServiceTest {

    @Mock
    private lateinit var amqpAdmin: AmqpAdmin

    @Mock
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var service: DlqAdminService

    @BeforeEach
    fun setUp() {
        service = DlqAdminService(amqpAdmin, rabbitTemplate)
    }

    @Test
    @DisplayName("모든 DLQ 큐의 정보를 조회한다")
    fun getAllDlqInfo() {
        val friendProps = Properties().apply {
            put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 5)
            put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1)
        }
        val serviceProps = Properties().apply {
            put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 2)
            put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 0)
        }
        
        whenever(amqpAdmin.getQueueProperties(RabbitMQConfig.FRIEND_DLQ)).thenReturn(friendProps)
        whenever(amqpAdmin.getQueueProperties(RabbitMQConfig.SERVICE_DLQ)).thenReturn(serviceProps)

        val result = service.getAllDlqInfo()

        assertThat(result).hasSize(2)
        val friendQueue = result.find { it.queueName == RabbitMQConfig.FRIEND_DLQ }
        assertThat(friendQueue?.messageCount).isEqualTo(5L)
        assertThat(friendQueue?.consumerCount).isEqualTo(1L)
    }

    @Test
    @DisplayName("알 수 없는 DLQ를 조회하면 예외가 발생한다")
    fun requireKnownDlq_fails() {
        assertThatThrownBy { service.getQueueInfo("UNKNOWN_QUEUE") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("DLQ 메시지를 꺼내서 원본 Exchange로 재발행한다")
    fun replayMessages() {
        val message1 = org.mockito.Mockito.mock(Message::class.java)
        val properties = org.mockito.Mockito.mock(MessageProperties::class.java)
        whenever(properties.receivedRoutingKey).thenReturn("original.key")
        whenever(message1.messageProperties).thenReturn(properties)
        
        whenever(rabbitTemplate.receive(RabbitMQConfig.FRIEND_DLQ))
            .thenReturn(message1)
            .thenReturn(null) // 1개 조회 후 종료

        val result = service.replayMessages(RabbitMQConfig.FRIEND_DLQ, 5)

        assertThat(result.queueName).isEqualTo(RabbitMQConfig.FRIEND_DLQ)
        assertThat(result.replayedCount).isEqualTo(1)
        verify(rabbitTemplate).send(RabbitMQConfig.EXCHANGE_NAME, "original.key", message1)
    }

    @Test
    @DisplayName("DLQ의 큐를 비운다")
    fun purgeQueue() {
        service.purgeQueue(RabbitMQConfig.FRIEND_DLQ)
        verify(amqpAdmin).purgeQueue(RabbitMQConfig.FRIEND_DLQ)
    }
}
