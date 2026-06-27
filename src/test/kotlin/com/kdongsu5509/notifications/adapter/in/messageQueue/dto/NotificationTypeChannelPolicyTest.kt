package com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationTypeChannelPolicyTest {

    @Test
    @DisplayName("도착/출발 알림은 critical 채널을 사용한다")
    fun channelId_critical() {
        assertThat(NotificationType.ARRIVAL.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(NotificationType.DEPARTURE.androidChannelId).isEqualTo("fcm_critical_channel")
    }

    @Test
    @DisplayName("친구 요청/위치 공유는 high 채널을 사용한다")
    fun channelId_high() {
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.androidChannelId).isEqualTo("fcm_high_channel")
        assertThat(NotificationType.LOCATION_SHARE_RECEIVED.androidChannelId).isEqualTo("fcm_high_channel")
    }

    @Test
    @DisplayName("친구 수락/발송 실패는 normal 채널을 사용한다")
    fun channelId_normal() {
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.androidChannelId).isEqualTo("fcm_normal_channel")
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.androidChannelId).isEqualTo("fcm_normal_channel")
    }

    @Test
    @DisplayName("공지/발송 결과는 silent 채널을 사용한다")
    fun channelId_silent() {
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.androidChannelId).isEqualTo("fcm_silent_channel")
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.androidChannelId).isEqualTo("fcm_silent_channel")
    }
}
