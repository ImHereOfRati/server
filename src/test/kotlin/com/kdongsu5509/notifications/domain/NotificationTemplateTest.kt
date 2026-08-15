package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class NotificationTemplateTest {

    private val placeData = mapOf(NotificationTemplate.PLACE_NAME_KEY to "학교")

    @Nested
    @DisplayName("본문 조립")
    inner class Body {

        @Test
        @DisplayName("친구 요청 본문은 발송자를 부르는 이름을 넣는다")
        fun render_success_friend_request() {
            val rendered = NotificationTemplate.render(NotificationType.FRIEND_REQUEST_RECEIVED, "홍길동")

            assertThat(rendered.title).isEqualTo("새로운 친구 요청")
            assertThat(rendered.body).isEqualTo("홍길동님이 친구 요청을 보냈습니다.")
        }

        @Test
        @DisplayName("도착 본문은 전달받은 장소명을 넣는다")
        fun render_success_arrival_uses_place_name() {
            val rendered = NotificationTemplate.render(NotificationType.ARRIVAL, "길동이", placeData)

            assertThat(rendered.body).isEqualTo("길동이님이 학교에 도착했습니다.")
        }

        @Test
        @DisplayName("공지처럼 발송자와 무관한 알림은 이름을 쓰지 않는다")
        fun render_success_notice_ignores_sender() {
            val rendered = NotificationTemplate.render(NotificationType.TERMS_UPDATE_NOTICE, "ImHere")

            assertThat(rendered.body).isEqualTo("이용약관이 업데이트되었습니다. 내용을 확인해 주세요.")
        }

        @ParameterizedTest
        @EnumSource(
            NotificationType::class,
            names = ["LOCATION_TARGET", "ARRIVAL", "DEPARTURE"],
        )
        @DisplayName("장소가 필요한 알림에 장소명이 없으면 조립을 거부한다")
        fun render_fail_when_place_name_missing(type: NotificationType) {
            assertThatThrownBy { NotificationTemplate.render(type, "길동이") }
                .isInstanceOf(InvalidInputException::class.java)
        }
    }

    @Nested
    @DisplayName("푸시 data 와이어 계약")
    inner class Data {

        @Test
        @DisplayName("추가 데이터에 종류와 부르는 이름을 얹어 내려보낸다")
        fun render_success_data_carries_type_and_alias() {
            val rendered = NotificationTemplate.render(NotificationType.ARRIVAL, "길동이", placeData)

            assertThat(rendered.data)
                .containsEntry("type", "ARRIVAL")
                .containsEntry("senderAlias", "길동이")
                .containsEntry(NotificationTemplate.PLACE_NAME_KEY, "학교")
        }

        @Test
        @DisplayName("딥링크 경로는 서버가 만들지 않는다 - 클라이언트가 종류로 정한다")
        fun render_success_carries_no_path() {
            val rendered = NotificationTemplate.render(NotificationType.FRIEND_REQUEST_RECEIVED, "홍길동")

            assertThat(rendered.data).doesNotContainKey("path")
        }

        @Test
        @DisplayName("요청용 body/path와 레거시 발신자 필드는 FCM data에 전달하지 않는다")
        fun render_strips_request_only_fields() {
            val rendered = NotificationTemplate.render(
                NotificationType.ARRIVAL,
                "길동이",
                mapOf(
                    "body" to "클라이언트 본문",
                    "path" to "/legacy/path",
                    "senderNickname" to "레거시 이름",
                    "senderEmail" to "legacy@example.com",
                    "placeName" to "학교",
                ),
            )

            assertThat(rendered.data)
                .containsOnlyKeys("type", "senderAlias", "placeName")
                .containsEntry("placeName", "학교")
        }
    }

    @Nested
    @DisplayName("모든 종류 지원")
    inner class Coverage {

        @ParameterizedTest
        @EnumSource(NotificationType::class)
        @DisplayName("어떤 종류든 제목과 본문이 비어 있지 않다")
        fun render_success_for_every_type(type: NotificationType) {
            val rendered = NotificationTemplate.render(type, "길동이", placeData)

            assertThat(rendered.title).isNotBlank()
            assertThat(rendered.body).isNotBlank()
            assertThat(rendered.type).isEqualTo(type)
            assertThat(rendered.channel).isEqualTo(PushChannel.of(type))
        }
    }
}
