package com.kdongsu5509.notifications.domain

enum class NotificationStatus {
    PENDING, // 발송 요청 접수
    SENT, // 발송 성공
    FAILED, // 발송 실패, 재시도 가능
    DEAD; // 발송 실패, 재시도 불가

    fun isSendable(): Boolean {
        return this == PENDING || this == FAILED
    }
}
