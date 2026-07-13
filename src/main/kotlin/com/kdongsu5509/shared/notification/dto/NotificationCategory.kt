package com.kdongsu5509.shared.notification.dto

import com.kdongsu5509.support.config.RabbitMQConfig

/** 도착/출발 알림 본문·경로에 필요한 장소명 키. 소비 측 NotificationType.PLACE_NAME_KEY와 값이 일치해야 한다(wire 계약). */
const val PLACE_NAME_KEY = "placeName"

/**
 * 알림 분류. 각 분류가 자신의 RabbitMQ 라우팅 키를 스스로 안다(라우팅 지식의 주인 = 분류 자신).
 *
 * 과거에는 어댑터의 `when(category)`가 라우팅 키를 외부에서 결정했으나(Type Code 스위치),
 * enum 프로퍼티로 흡수해 새 분류 추가가 이 한곳으로 끝나게 한다. 라우팅 키 상수값은 [RabbitMQConfig]가 소유한다.
 *
 * [requiredDataKeys]는 이 분류가 페이로드([NotificationQueueMessage.data])에 반드시 담아야 하는 키다.
 * 소비 측([NotificationType])이 렌더링 시점에 요구하던 필수 데이터를 발행 측에서도 알게 해,
 * 누락을 소비 시점(500→DLQ)이 아니라 발행 시점에 fail-fast로 거른다(계약 소유자=wire DTO).
 */
enum class NotificationCategory(
    val routingKey: String,
    val requiredDataKeys: Set<String> = emptySet(),
) {
    FRIEND_REQUEST_RECEIVED(RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED),
    FRIEND_REQUEST_ACCEPTED(RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_ACCEPTED),
    ARRIVAL_CONFIRMATION(RabbitMQConfig.ROUTING_KEY_ARRIVAL_CONFIRMATION, requiredDataKeys = setOf(PLACE_NAME_KEY)),
    TERMS_UPDATE_NOTICE(RabbitMQConfig.ROUTING_KEY_TERMS_UPDATE),
    DELIVERY_RESULT_NOTICE(RabbitMQConfig.ROUTING_KEY_DELIVERY_RESULT),
}
