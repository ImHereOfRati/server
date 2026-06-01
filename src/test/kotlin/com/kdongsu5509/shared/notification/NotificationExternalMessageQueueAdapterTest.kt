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

    @ParameterizedTest
    @CsvSource(
        "FRIEND_REQUEST_RECEIVED, noti.friend.request.received",
        "FRIEND_REQUEST_ACCEPTED, noti.friend.request.accepted",
        "LOCATION_SHARE_RECEIVED, noti.service.location.share",
        "ARRIVAL_CONFIRMATION, noti.service.location.arrival",
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
