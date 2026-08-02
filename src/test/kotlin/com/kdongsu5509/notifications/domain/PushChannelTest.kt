package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PushChannelTest {

    @Test
    @DisplayName("놓치면 안 되는 알림은 집중 모드를 뚫는다")
    fun critical_breaks_through_focus_mode() {
        // given
        val channel = PushChannel.CRITICAL

        // then
        assertThat(channel.androidChannelId).isEqualTo("fcm_critical_channel")
        assertThat(channel.androidPriority).isEqualTo(AndroidPushPriority.HIGH)
        assertThat(channel.apnsPriority).isEqualTo(ApnsPushPriority.IMMEDIATE)
        assertThat(channel.interruptionLevel).isEqualTo(IosInterruptionLevel.TIME_SENSITIVE)
        assertThat(channel.isSilent).isFalse()
    }

    @Test
    @DisplayName("바로 알려야 하는 알림은 즉시 전달하되 집중 모드는 존중한다")
    fun high_delivers_immediately() {
        // given
        val channel = PushChannel.HIGH

        // then
        assertThat(channel.androidChannelId).isEqualTo("fcm_high_channel")
        assertThat(channel.androidPriority).isEqualTo(AndroidPushPriority.HIGH)
        assertThat(channel.apnsPriority).isEqualTo(ApnsPushPriority.IMMEDIATE)
        assertThat(channel.interruptionLevel).isEqualTo(IosInterruptionLevel.ACTIVE)
        assertThat(channel.isSilent).isFalse()
    }

    @Test
    @DisplayName("급하지 않은 알림도 즉시 전달하되 방해 수준은 보통이다")
    fun normal_delivers_immediately_with_active_level() {
        // given
        val channel = PushChannel.NORMAL

        // then
        assertThat(channel.androidChannelId).isEqualTo("fcm_normal_channel")
        assertThat(channel.androidPriority).isEqualTo(AndroidPushPriority.HIGH)
        assertThat(channel.apnsPriority).isEqualTo(ApnsPushPriority.IMMEDIATE)
        assertThat(channel.interruptionLevel).isEqualTo(IosInterruptionLevel.ACTIVE)
        assertThat(channel.isSilent).isFalse()
    }

    @Test
    @DisplayName("공지성 알림은 소리 없이 조용히 쌓인다")
    fun silent_arrives_without_disturbing() {
        // given
        val channel = PushChannel.SILENT

        // then
        assertThat(channel.androidChannelId).isEqualTo("fcm_silent_channel")
        assertThat(channel.androidPriority).isEqualTo(AndroidPushPriority.NORMAL)
        assertThat(channel.apnsPriority).isEqualTo(ApnsPushPriority.THROTTLED)
        assertThat(channel.interruptionLevel).isEqualTo(IosInterruptionLevel.PASSIVE)
        assertThat(channel.isSilent).isTrue()
        assertThat(channel.sound).isNull()
    }

    @Test
    @DisplayName("소리가 있는 채널만 알림음을 지정한다")
    fun sound_is_set_only_for_non_silent_channels() {
        PushChannel.entries.forEach { channel ->
            if (channel.isSilent) assertThat(channel.sound).isNull()
            else assertThat(channel.sound).isNotBlank()
        }
    }

    @Test
    @DisplayName("APNs 헤더와 방해 수준은 애플이 정한 문자열을 그대로 쓴다")
    fun apns_values_match_apple_spec() {
        assertThat(ApnsPushPriority.IMMEDIATE.headerValue).isEqualTo("10")
        assertThat(ApnsPushPriority.THROTTLED.headerValue).isEqualTo("5")

        assertThat(IosInterruptionLevel.PASSIVE.value).isEqualTo("passive")
        assertThat(IosInterruptionLevel.ACTIVE.value).isEqualTo("active")
        assertThat(IosInterruptionLevel.TIME_SENSITIVE.value).isEqualTo("time-sensitive")
    }
}
