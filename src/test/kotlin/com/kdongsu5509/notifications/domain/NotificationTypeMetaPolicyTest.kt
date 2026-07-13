package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationTypeMetaPolicyTest {

    @Test
    @DisplayName("발송 결과/실패 알림만 메타 타입이다(후속 수신증을 유발하지 않는다)")
    fun metaTypesAreOnlyDeliveryResultAndFailed() {
        val metaTypes = NotificationType.entries.filter { it.isMeta }

        assertThat(metaTypes).containsExactlyInAnyOrder(
            NotificationType.DELIVERY_RESULT_NOTICE,
            NotificationType.DELIVERY_FAILED_NOTICE
        )
    }

    @Test
    @DisplayName("일반 발송 타입은 메타 타입이 아니다")
    fun nonMetaTypesAreNotMeta() {
        assertThat(NotificationType.FRIEND_REQUEST_RECEIVED.isMeta).isFalse()
        assertThat(NotificationType.FRIEND_REQUEST_ACCEPTED.isMeta).isFalse()
        assertThat(NotificationType.LOCATION_TARGET.isMeta).isFalse()
        assertThat(NotificationType.ARRIVAL.isMeta).isFalse()
        assertThat(NotificationType.DEPARTURE.isMeta).isFalse()
        assertThat(NotificationType.ARRIVAL_CONFIRMATION.isMeta).isFalse()
        assertThat(NotificationType.TERMS_UPDATE_NOTICE.isMeta).isFalse()
    }
}
