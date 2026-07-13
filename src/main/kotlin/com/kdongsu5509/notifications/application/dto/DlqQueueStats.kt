package com.kdongsu5509.notifications.application.dto

/** 메시지 브로커에서 조회한 DLQ 원시 통계. [DlqManagementPort]가 반환하는 경계 값. */
data class DlqQueueStats(
    val messageCount: Long,
    val consumerCount: Long
)
