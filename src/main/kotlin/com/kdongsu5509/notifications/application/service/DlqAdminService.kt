package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.DlqQueueInfo
import com.kdongsu5509.notifications.application.dto.DlqReplayResult
import com.kdongsu5509.notifications.application.port.out.DlqManagementPort
import com.kdongsu5509.support.config.RabbitMQConfig
import org.springframework.stereotype.Service

@Service
class DlqAdminService(
    private val dlqManagementPort: DlqManagementPort
) {
    companion object {
        /** DLQ → 재발행 exchange 매핑 */
        val DLQ_REPLAY_TARGET: Map<String, String> = mapOf(
            RabbitMQConfig.FRIEND_DLQ to RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.SERVICE_DLQ to RabbitMQConfig.EXCHANGE_NAME
        )
    }

    fun getAllDlqInfo(): List<DlqQueueInfo> =
        DLQ_REPLAY_TARGET.keys.map { getQueueInfo(it) }

    fun getQueueInfo(queueName: String): DlqQueueInfo {
        requireKnownDlq(queueName)
        val stats = dlqManagementPort.getQueueStats(queueName)
            ?: error("DLQ 큐를 찾을 수 없습니다: $queueName")
        return DlqQueueInfo(
            queueName = queueName,
            messageCount = stats.messageCount,
            consumerCount = stats.consumerCount
        )
    }

    /**
     * DLQ에서 최대 [count]개의 메시지를 꺼내 원본 Exchange로 재발행한다.
     * [count]가 null이면 DLQ의 모든 메시지를 재발행한다.
     * 재발행된 메시지는 DLQ에서 소비(제거)된다.
     */
    fun replayMessages(queueName: String, count: Int? = null): DlqReplayResult {
        requireKnownDlq(queueName)
        val exchange = DLQ_REPLAY_TARGET.getValue(queueName)
        val limit = count ?: Int.MAX_VALUE

        var replayed = 0
        repeat(limit) {
            if (!dlqManagementPort.replayOne(queueName, exchange)) {
                return DlqReplayResult(queueName, replayed)
            }
            replayed++
        }
        return DlqReplayResult(queueName, replayed)
    }

    /**
     * DLQ의 모든 메시지를 삭제한다.
     */
    fun purgeQueue(queueName: String) {
        requireKnownDlq(queueName)
        dlqManagementPort.purge(queueName)
    }

    private fun requireKnownDlq(queueName: String) {
        require(DLQ_REPLAY_TARGET.containsKey(queueName)) {
            "알 수 없는 DLQ입니다: $queueName. 허용 목록: ${DLQ_REPLAY_TARGET.keys}"
        }
    }
}
