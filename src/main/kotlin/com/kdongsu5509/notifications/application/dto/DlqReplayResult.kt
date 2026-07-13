package com.kdongsu5509.notifications.application.dto

/** DLQ 재발행 결과의 application 타입. 표현(웹 DTO)은 어댑터가 매핑한다. */
data class DlqReplayResult(
    val queueName: String,
    val replayedCount: Int
)
