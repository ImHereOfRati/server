package com.kdongsu5509.notifications.domain

/**
 * 알림 한 건의 발송 생애주기.
 *
 * 외부 큐 위치에 기대지 않고 발송 생애주기를 영속화한다.
 * 브로커를 걷어내면서 "지금 이 알림이 어디까지 갔는가"를 도메인이 직접 들고 있게 한다.
 *
 * [DEAD]는 재시도를 모두 소진해 운영자가 손을 대야 하는 상태다.
 */
enum class NotificationStatus {
    /** 발송 요청이 접수됐다. 아직 외부 채널로 나가지 않았다. */
    PENDING,

    /** 외부 채널(FCM/SMS) 발송에 성공했다. */
    SENT,

    /** 발송에 실패했지만 아직 재시도할 수 있다. */
    FAILED,

    /** 재시도를 모두 소진했다. 운영자가 재발송하거나 폐기해야 한다. */
    DEAD,
    ;

    /** 지금 발송을 시도할 수 있는 상태인가. */
    val isDeliverable: Boolean
        get() = this == PENDING || this == FAILED
}
