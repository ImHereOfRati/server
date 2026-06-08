package com.kdongsu5509.notifications.adapter.`in`.messageQueue

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationMessageDto
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.shared.notification.dto.NotificationPersonInfo
import com.kdongsu5509.support.config.RabbitMQConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.LocalDateTime
import java.util.*

class NotificationConsumerIntegrationTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @MockitoBean
    private lateinit var notificationDispatcherUseCase: NotificationDispatcherUseCase

    @Test
    @DisplayName("RabbitMQ를 통해 친구 알림 메시지가 정상적으로 컨슈머에 도달한다")
    fun consumeFriendNotification() {
        // given
        val messageDto = NotificationMessageDto(
            category = NotificationType.FRIEND_REQUEST_RECEIVED,
            sender = NotificationPersonInfo("sender@test.com", "senderNick"),
            receiver = NotificationPersonInfo("receiver@test.com", "receiverNick"),
            data = mapOf("key" to "value"),
            timestamp = LocalDateTime.now(),
            messageId = UUID.randomUUID()
        )

        // when
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_FRIEND_REQUEST_RECEIVED,
            messageDto
        )

        // then
        verify(notificationDispatcherUseCase, timeout(3000).times(1)).dispatch(any())
    }

    @Test
    @DisplayName("RabbitMQ를 통해 서비스 알림 메시지가 정상적으로 컨슈머에 도달한다")
    fun consumeServiceNotification() {
        // given
        val messageDto = NotificationMessageDto(
            category = NotificationType.TERMS_UPDATE_NOTICE,
            sender = NotificationPersonInfo("admin@imhere.com", "Admin"),
            receiver = NotificationPersonInfo("user@test.com", "User"),
            data = null,
            timestamp = LocalDateTime.now(),
            messageId = UUID.randomUUID()
        )

        // when
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY_TERMS_UPDATE,
            messageDto
        )

        // then
        verify(notificationDispatcherUseCase, timeout(3000).times(1)).dispatch(any())
    }
}
