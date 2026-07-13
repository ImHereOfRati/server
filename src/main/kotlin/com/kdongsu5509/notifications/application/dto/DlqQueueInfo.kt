package com.kdongsu5509.notifications.application.dto

/** DLQ 큐 상태(이름·메시지 수·컨슈머 수)의 application 결과 타입. 표현(웹 DTO)은 어댑터가 매핑한다. */
data class DlqQueueInfo(
    val queueName: String,
    val messageCount: Long,
    val consumerCount: Long
)
