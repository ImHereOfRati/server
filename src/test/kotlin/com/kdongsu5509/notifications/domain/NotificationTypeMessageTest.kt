package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationTypeMessageTest {

    @Test
    @DisplayName("각 알림 종류는 자신의 제목을 안다")
    fun titleText() {
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.titleText).isEqualTo("새로운 친구 요청")
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.titleText).isEqualTo("친구 요청 수락")
        assertThat(NotificationType.LOCATION_SHARE_RECEIVED.titleText).isEqualTo("위치 공유 알림")
        assertThat(NotificationType.ARRIVAL.titleText).isEqualTo("도착 안내")
        assertThat(NotificationType.DEPARTURE.titleText).isEqualTo("출발 안내")
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.titleText).isEqualTo("서비스 공지사항")
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.titleText).isEqualTo("발송 결과 알림")
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.titleText).isEqualTo("발송 실패 알림")
    }

    @Test
    @DisplayName("장소가 필요 없는 알림은 발송자 닉네임으로 본문을 만든다")
    fun bodyText_withoutPlace() {
        val sender = "테스터"
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.bodyText(sender))
            .isEqualTo("${sender}님이 친구 요청을 보냈습니다.")
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.bodyText(sender))
            .isEqualTo("${sender}님이 친구 요청을 수락했습니다.")
        assertThat(NotificationType.LOCATION_SHARE_RECEIVED.bodyText(sender))
            .isEqualTo("${sender}님이 위치를 공유했습니다.")
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.bodyText(sender))
            .isEqualTo("이용약관이 업데이트되었습니다. 내용을 확인해 주세요.")
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.bodyText(sender))
            .isEqualTo("요청하신 발송 작업이 완료되었습니다.")
    }

    @Test
    @DisplayName("도착 알림 본문은 발송자와 장소를 포함한다")
    fun bodyText_arrival_includesPlace() {
        val data = mapOf(NotificationType.PLACE_NAME_KEY to "우리집")
        assertThat(NotificationType.ARRIVAL.bodyText("민수", data))
            .isEqualTo("민수님이 우리집에 도착했습니다.")
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.bodyText("민수", data))
            .isEqualTo("민수님이 우리집에 도착했습니다.")
    }

    @Test
    @DisplayName("출발 알림 본문은 발송자와 출발 장소를 포함한다")
    fun bodyText_departure_includesPlace() {
        val data = mapOf(NotificationType.PLACE_NAME_KEY to "회사")
        assertThat(NotificationType.DEPARTURE.bodyText("민수", data))
            .isEqualTo("민수님이 회사에서 출발했습니다.")
    }

    @Test
    @DisplayName("도착/출발 알림에 장소가 없으면 예외가 발생한다")
    fun bodyText_arrivalDeparture_missingPlace_fails() {
        assertThatThrownBy { NotificationType.ARRIVAL.bodyText("민수", emptyMap()) }
            .isInstanceOf(InvalidInputException::class.java)
        assertThatThrownBy { NotificationType.DEPARTURE.bodyText("민수", mapOf(NotificationType.PLACE_NAME_KEY to " ")) }
            .isInstanceOf(InvalidInputException::class.java)
    }
}
