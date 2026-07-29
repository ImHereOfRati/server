package com.kdongsu5509.notifications.domain

/**
 * 하나의 발송 대상으로 렌더링된 알림.
 *
 * [NotificationType.render]가 발송자·추가 데이터로부터 제목/본문/딥링크 경로/푸시 data 맵을 한 번에 조립해 만든다.
 * 발송 페이로드(title/body/data)의 조립 지식을 소유해 서비스가 흐름 제어에 집중하게 한다.
 *
 * [data]의 키(`type`/`path`/`senderNickname`/`senderEmail`)는 FCM data 와이어 계약이며 어댑터가 그대로 읽는다.
 */
data class RenderedNotification(
    val type: NotificationType,
    val senderNickname: String,
    val title: String,
    val body: String,
    val path: String,
    val data: Map<String, String>,
)
