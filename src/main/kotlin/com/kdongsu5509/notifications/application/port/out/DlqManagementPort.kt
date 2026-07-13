package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.application.dto.DlqQueueStats

/**
 * DLQ(Dead Letter Queue) 관리 out-port.
 *
 * 큐 통계 조회·단건 재발행·비우기를 추상화해, 메시지 브로커(Spring AMQP) 세부가 application 계층으로 새지 않게 한다.
 * 재발행은 [replayOne]으로 한 건씩 위임하며(브로커 Message 타입을 어댑터 안에 가둔다), 재발행 정책(개수 제한 등)은 서비스가 소유한다.
 */
interface DlqManagementPort {
    /** 큐 통계를 조회한다. 큐가 없으면 null. */
    fun getQueueStats(queueName: String): DlqQueueStats?

    /** [queueName]에서 한 건을 꺼내 원본 라우팅 키로 [targetExchange]에 재발행한다. 꺼낼 메시지가 없으면 false. */
    fun replayOne(queueName: String, targetExchange: String): Boolean

    /** 큐의 모든 메시지를 삭제한다. */
    fun purge(queueName: String)
}
