package com.kdongsu5509.notifications.adapter.out.messageQueue

import com.kdongsu5509.support.config.RabbitMQConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.amqp.core.AmqpAdmin
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.Properties

@ExtendWith(MockitoExtension::class)
class DlqManagementAdapterTest {

    @Mock
    private lateinit var amqpAdmin: AmqpAdmin

    @Mock
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var adapter: DlqManagementAdapter

    @BeforeEach
    fun setUp() {
        adapter = DlqManagementAdapter(amqpAdmin, rabbitTemplate)
    }

    @Test
    @DisplayName("큐 속성에서 메시지 수/컨슈머 수를 읽어 통계로 반환한다")
    fun getQueueStats_success() {
        val props = Properties().apply {
            put(RabbitAdmin.QUEUE_MESSAGE_COUNT, 5)
            put(RabbitAdmin.QUEUE_CONSUMER_COUNT, 1)
        }
        whenever(amqpAdmin.getQueueProperties(RabbitMQConfig.FRIEND_DLQ)).thenReturn(props)

        val stats = adapter.getQueueStats(RabbitMQConfig.FRIEND_DLQ)

        assertThat(stats?.messageCount).isEqualTo(5L)
        assertThat(stats?.consumerCount).isEqualTo(1L)
    }

    @Test
    @DisplayName("큐 속성이 없으면 null을 반환한다")
    fun getQueueStats_null() {
        whenever(amqpAdmin.getQueueProperties(RabbitMQConfig.FRIEND_DLQ)).thenReturn(null)

        assertThat(adapter.getQueueStats(RabbitMQConfig.FRIEND_DLQ)).isNull()
    }

    @Test
    @DisplayName("메시지를 꺼내 원본 라우팅 키로 대상 Exchange에 재발행하면 true를 반환한다")
    fun replayOne_success() {
        val message = org.mockito.Mockito.mock(Message::class.java)
        val properties = org.mockito.Mockito.mock(MessageProperties::class.java)
        whenever(properties.receivedRoutingKey).thenReturn("original.key")
        whenever(message.messageProperties).thenReturn(properties)
        whenever(rabbitTemplate.receive(RabbitMQConfig.FRIEND_DLQ)).thenReturn(message)

        val replayed = adapter.replayOne(RabbitMQConfig.FRIEND_DLQ, RabbitMQConfig.EXCHANGE_NAME)

        assertThat(replayed).isTrue()
        verify(rabbitTemplate).send(RabbitMQConfig.EXCHANGE_NAME, "original.key", message)
    }

    @Test
    @DisplayName("꺼낼 메시지가 없으면 false를 반환하고 재발행하지 않는다")
    fun replayOne_empty() {
        whenever(rabbitTemplate.receive(RabbitMQConfig.FRIEND_DLQ)).thenReturn(null)

        val replayed = adapter.replayOne(RabbitMQConfig.FRIEND_DLQ, RabbitMQConfig.EXCHANGE_NAME)

        assertThat(replayed).isFalse()
    }

    @Test
    @DisplayName("큐를 비운다")
    fun purge() {
        adapter.purge(RabbitMQConfig.FRIEND_DLQ)
        verify(amqpAdmin).purgeQueue(RabbitMQConfig.FRIEND_DLQ)
    }
}
