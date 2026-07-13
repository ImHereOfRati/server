package com.kdongsu5509.shared.notification.dto

import com.kdongsu5509.support.config.RabbitMQConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationCategoryTest {

    @Test
    @DisplayName("각 분류가 자신의 라우팅 키를 안다 (P1 - 어댑터 when 스위치 흡수)")
    fun routingKey() {
        assertThat(NotificationCategory.FRIEND_REQUEST_RECEIVED.routingKey)
            .isEqualTo(RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED)
        assertThat(NotificationCategory.FRIEND_REQUEST_ACCEPTED.routingKey)
            .isEqualTo(RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_ACCEPTED)
        assertThat(NotificationCategory.ARRIVAL_CONFIRMATION.routingKey)
            .isEqualTo(RabbitMQConfig.ROUTING_KEY_ARRIVAL_CONFIRMATION)
        assertThat(NotificationCategory.TERMS_UPDATE_NOTICE.routingKey)
            .isEqualTo(RabbitMQConfig.ROUTING_KEY_TERMS_UPDATE)
        assertThat(NotificationCategory.DELIVERY_RESULT_NOTICE.routingKey)
            .isEqualTo(RabbitMQConfig.ROUTING_KEY_DELIVERY_RESULT)
    }

    @Test
    @DisplayName("도착 확인 분류는 장소명을 필수 데이터로 요구한다 (P5 - 발행 측 정합)")
    fun requiredDataKeys() {
        assertThat(NotificationCategory.ARRIVAL_CONFIRMATION.requiredDataKeys)
            .containsExactly(PLACE_NAME_KEY)
        assertThat(NotificationCategory.FRIEND_REQUEST_RECEIVED.requiredDataKeys).isEmpty()
    }
}
