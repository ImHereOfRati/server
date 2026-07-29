package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 알림 종류가 어떤 전달 정책을 타는지 고정한다.
 *
 * [PushChannel] 도입으로 채널 지정 방식이 바뀌었으므로, 종류별 실제 매핑이 예전과 달라지지 않았음을 확인한다.
 */
class NotificationTypeChannelPolicyTest {

    @Test
    @DisplayName("도착/출발 알림은 critical 채널을 사용한다")
    fun channel_critical() {
        assertThat(NotificationType.ARRIVAL.channel).isEqualTo(PushChannel.CRITICAL)
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.channel).isEqualTo(PushChannel.CRITICAL)
        assertThat(NotificationType.DEPARTURE.channel).isEqualTo(PushChannel.CRITICAL)
    }

    @Test
    @DisplayName("친구 요청/위치 공유는 high 채널을 사용한다")
    fun channel_high() {
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.channel).isEqualTo(PushChannel.HIGH)
        assertThat(NotificationType.LOCATION_TARGET.channel).isEqualTo(PushChannel.HIGH)
    }

    @Test
    @DisplayName("친구 수락/발송 실패는 normal 채널을 사용한다")
    fun channel_normal() {
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.channel).isEqualTo(PushChannel.NORMAL)
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.channel).isEqualTo(PushChannel.NORMAL)
    }

    @Test
    @DisplayName("공지/발송 결과는 silent 채널을 사용한다")
    fun channel_silent() {
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.channel).isEqualTo(PushChannel.SILENT)
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.channel).isEqualTo(PushChannel.SILENT)
    }

    @Test
    @DisplayName("Android 채널 ID는 PushChannel 도입 전과 같다")
    fun androidChannelId_unchanged() {
        assertThat(NotificationType.ARRIVAL.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(NotificationType.DEPARTURE.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.androidChannelId).isEqualTo("fcm_high_channel")
        assertThat(NotificationType.LOCATION_TARGET.androidChannelId).isEqualTo("fcm_high_channel")
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.androidChannelId).isEqualTo("fcm_normal_channel")
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.androidChannelId).isEqualTo("fcm_normal_channel")
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.androidChannelId).isEqualTo("fcm_silent_channel")
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.androidChannelId).isEqualTo("fcm_silent_channel")
    }

    @Test
    @DisplayName("Android 푸시 우선순위는 PushChannel 도입 전과 같다")
    fun pushPriority_unchanged() {
        listOf(
            NotificationType.FRIEND_REQUEST_RECEIVED,
            NotificationType.FRIEND_REQUEST_ACCEPTED,
            NotificationType.LOCATION_TARGET,
            NotificationType.ARRIVAL,
            NotificationType.DEPARTURE,
            NotificationType.ARRIVAL_CONFIRMATION,
            NotificationType.DELIVERY_FAILED_NOTICE,
        ).forEach { assertThat(it.pushPriority).isEqualTo(AndroidPushPriority.HIGH) }

        listOf(
            NotificationType.TERMS_UPDATE_NOTICE,
            NotificationType.DELIVERY_RESULT_NOTICE,
        ).forEach { assertThat(it.pushPriority).isEqualTo(AndroidPushPriority.NORMAL) }
    }

    @Test
    @DisplayName("발송 결과를 알리는 메타 타입은 조용한 채널로만 나간다")
    fun meta_types_use_quiet_channels() {
        // 수신증은 사용자가 요청한 알림이 아니므로 요란하게 울리면 안 된다.
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.channel.interruptionLevel)
            .isEqualTo(IosInterruptionLevel.PASSIVE)
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.channel.interruptionLevel)
            .isEqualTo(IosInterruptionLevel.ACTIVE)
    }
}
