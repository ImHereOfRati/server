package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationTypeRoutePolicyTest {

    @Test
    @DisplayName("알림 타입별 앱 경로가 모바일 라우트와 일치한다")
    fun appPathMatchesMobileRoutes() {
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.appPath).isEqualTo("/friend/requests")
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.appPath).isEqualTo("/friend")
        assertThat(NotificationType.LOCATION_SHARE_RECEIVED.appPath).isEqualTo("/record/notifications")
        assertThat(NotificationType.LOCATION_TARGET.appPath).isEqualTo("/record/notifications")
        assertThat(NotificationType.ARRIVAL.appPath).isEqualTo("/record/notifications")
        assertThat(NotificationType.DEPARTURE.appPath).isEqualTo("/record/notifications")
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.appPath).isEqualTo("/record/notifications")
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.appPath).isEqualTo("/terms-detail/{termId}")
        assertThat(NotificationType.DELIVERY_RESULT_NOTICE.appPath).isEqualTo("/record/send-history")
        assertThat(NotificationType.DELIVERY_FAILED_NOTICE.appPath).isEqualTo("/record/send-history")
    }
}
