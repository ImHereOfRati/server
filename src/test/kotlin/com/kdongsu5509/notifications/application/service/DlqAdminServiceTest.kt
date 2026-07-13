package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.DlqQueueStats
import com.kdongsu5509.notifications.application.port.out.DlqManagementPort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class DlqAdminServiceTest {

    @Mock
    private lateinit var dlqManagementPort: DlqManagementPort

    private lateinit var service: DlqAdminService

    @BeforeEach
    fun setUp() {
        service = DlqAdminService(dlqManagementPort)
    }

    @Test
    @DisplayName("모든 DLQ 큐의 정보를 조회한다")
    fun getAllDlqInfo() {
        whenever(dlqManagementPort.getQueueStats(RabbitMQConfig.FRIEND_DLQ))
            .thenReturn(DlqQueueStats(messageCount = 5L, consumerCount = 1L))
        whenever(dlqManagementPort.getQueueStats(RabbitMQConfig.SERVICE_DLQ))
            .thenReturn(DlqQueueStats(messageCount = 2L, consumerCount = 0L))

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

        verify(dlqManagementPort, never()).getQueueStats(org.mockito.kotlin.any())
    }

    @Test
    @DisplayName("DLQ 메시지를 꺼내서 원본 Exchange로 재발행한다")
    fun replayMessages() {
        whenever(dlqManagementPort.replayOne(RabbitMQConfig.FRIEND_DLQ, RabbitMQConfig.EXCHANGE_NAME))
            .thenReturn(true)
            .thenReturn(false) // 1건 재발행 후 종료

        val result = service.replayMessages(RabbitMQConfig.FRIEND_DLQ, 5)

        assertThat(result.queueName).isEqualTo(RabbitMQConfig.FRIEND_DLQ)
        assertThat(result.replayedCount).isEqualTo(1)
        verify(dlqManagementPort, org.mockito.kotlin.times(2))
            .replayOne(RabbitMQConfig.FRIEND_DLQ, RabbitMQConfig.EXCHANGE_NAME)
    }

    @Test
    @DisplayName("DLQ의 큐를 비운다")
    fun purgeQueue() {
        service.purgeQueue(RabbitMQConfig.FRIEND_DLQ)
        verify(dlqManagementPort).purge(RabbitMQConfig.FRIEND_DLQ)
    }
}
