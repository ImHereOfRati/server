package com.kdongsu5509.shared.notification

import com.kdongsu5509.shared.notification.dto.NotificationCategory
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import com.kdongsu5509.shared.notification.dto.NotificationQueueMessage
import com.kdongsu5509.shared.notification.dto.NotificationSendRequest
import com.kdongsu5509.support.config.RabbitMQConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.amqp.rabbit.core.RabbitTemplate

@ExtendWith(MockitoExtension::class)
class NotificationExternalMessageQueueAdapterTest {

    @Mock
    private lateinit var rabbitTemplate: RabbitTemplate

    private lateinit var adapter: NotificationExternalMessageQueueAdapter

    @BeforeEach
    fun setUp() {
        adapter = NotificationExternalMessageQueueAdapter(rabbitTemplate)
    }

    // 라우팅 키 전수 매핑은 NotificationCategoryTest.routingKey가 담당한다.
    // 여기서는 데이터 없이 발행 가능한(requiredDataKeys 없는) 분류만 어댑터 경로로 검증한다.
    // ARRIVAL_CONFIRMATION 등 필수 데이터가 있는 분류는 data-less 어댑터(send)로 발행할 수 없다(발행 측 fail-fast).
    @ParameterizedTest
    @CsvSource(
        "FRIEND_REQUEST_RECEIVED, noti.friend.request.received",
        "FRIEND_REQUEST_ACCEPTED, noti.friend.request.accepted",
        "TERMS_UPDATE_NOTICE, noti.service.terms.update",
        "DELIVERY_RESULT_NOTICE, noti.service.delivery.result"
    )
    @DisplayName("카테고리에 맞는 라우팅 키로 메시지를 발송한다")
    fun send(category: NotificationCategory, expectedRoutingKey: String) {
        val request = NotificationSendRequest(
            category = category,
            sender = NotificationPersonInfo("sender@test.com", "sender"),
            receiver = NotificationPersonInfo("receiver@test.com", "receiver")
        )

        adapter.send(request)

        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMQConfig.EXCHANGE_NAME),
            eq(expectedRoutingKey),
            any<NotificationQueueMessage>()
        )
    }
}
