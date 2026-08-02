package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class NotificationTypePolicyTest {

    @Nested
    @DisplayName("메타 타입 판정")
    inner class Meta {

        @Test
        @DisplayName("발송 결과를 알리는 종류만 메타다")
        fun isMeta_success() {
            assertThat(NotificationType.entries.filter { it.isMeta })
                .containsExactlyInAnyOrder(
                    NotificationType.DELIVERY_RESULT_NOTICE,
                    NotificationType.DELIVERY_FAILED_NOTICE,
                )
        }
    }

    @Nested
    @DisplayName("클라이언트 직접 발송 허용")
    inner class ClientAllowed {

        @Test
        @DisplayName("위치 관련 알림만 클라이언트가 직접 요청할 수 있다")
        fun clientAllowed_success() {
            assertThat(NotificationType.CLIENT_ALLOWED)
                .containsExactlyInAnyOrder(
                    NotificationType.LOCATION_TARGET,
                    NotificationType.ARRIVAL,
                    NotificationType.DEPARTURE,
                )
        }

        @Test
        @DisplayName("수신증은 서버만 발행한다 - 클라이언트가 흉내 낼 수 없다")
        fun clientAllowed_excludes_meta() {
            assertThat(NotificationType.CLIENT_ALLOWED.none { it.isMeta }).isTrue()
        }
    }

    @Nested
    @DisplayName("푸시 전달 정책 대응")
    inner class Channel {

        @ParameterizedTest
        @EnumSource(NotificationType::class)
        @DisplayName("모든 종류가 푸시 정책 하나에 대응된다")
        fun of_success_for_every_type(type: NotificationType) {
            assertThat(PushChannel.of(type)).isNotNull()
        }

        @Test
        @DisplayName("놓치면 안 되는 도착·출발은 집중 모드를 뚫는 정책을 쓴다")
        fun of_success_arrival_departure_is_critical() {
            assertThat(PushChannel.of(NotificationType.ARRIVAL)).isEqualTo(PushChannel.CRITICAL)
            assertThat(PushChannel.of(NotificationType.DEPARTURE)).isEqualTo(PushChannel.CRITICAL)
        }

        @Test
        @DisplayName("공지와 발송 결과는 조용히 쌓인다")
        fun of_success_notices_are_silent() {
            assertThat(PushChannel.of(NotificationType.TERMS_UPDATE_NOTICE).isSilent).isTrue()
            assertThat(PushChannel.of(NotificationType.DELIVERY_RESULT_NOTICE).isSilent).isTrue()
        }
    }
}
