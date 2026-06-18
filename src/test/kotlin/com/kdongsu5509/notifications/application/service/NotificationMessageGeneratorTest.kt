package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationMessageGeneratorTest {

    @Test
    @DisplayName("NotificationType에 맞는 메시지 제목을 반환한다")
    fun getMessageTitle() {
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.FRIEND_REQUEST_RECEIVED))
            .isEqualTo("새로운 친구 요청")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.FRIEND_REQUEST_ACCEPTED))
            .isEqualTo("친구 요청 수락")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.LOCATION_SHARE_RECEIVED))
            .isEqualTo("위치 공유 알림")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.ARRIVAL_CONFIRMATION))
            .isEqualTo("도착 안내")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.DEPARTURE))
            .isEqualTo("출발 안내")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.TERMS_UPDATE_NOTICE))
            .isEqualTo("서비스 공지사항")
        assertThat(NotificationMessageGenerator.getMessageTitle(NotificationType.DELIVERY_RESULT_NOTICE))
            .isEqualTo("발송 결과 알림")
    }

    @Test
    @DisplayName("NotificationType에 맞는 메시지 본문을 반환한다")
    fun getMessageBody() {
        val sender = "테스터"
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.FRIEND_REQUEST_RECEIVED, sender))
            .isEqualTo("${sender}님이 친구 요청을 보냈습니다.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.FRIEND_REQUEST_ACCEPTED, sender))
            .isEqualTo("${sender}님이 친구 요청을 수락했습니다.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.LOCATION_SHARE_RECEIVED, sender))
            .isEqualTo("${sender}님이 위치를 공유했습니다.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.ARRIVAL_CONFIRMATION, sender))
            .isEqualTo("${sender}님이 도착했습니다.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.DEPARTURE, sender))
            .isEqualTo("${sender}님이 출발했습니다.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.TERMS_UPDATE_NOTICE, sender))
            .isEqualTo("이용약관이 업데이트되었습니다. 내용을 확인해 주세요.")
        assertThat(NotificationMessageGenerator.getMessageBody(NotificationType.DELIVERY_RESULT_NOTICE, sender))
            .isEqualTo("요청하신 발송 작업이 완료되었습니다.")
    }
}
