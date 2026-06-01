package com.kdongsu5509.shared.notification.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationQueueMessageTest {

    @Test
    @DisplayName("NotificationSendRequest로부터 QueueMessage를 생성한다")
    fun from() {
        val request = NotificationSendRequest(
            category = NotificationCategory.FRIEND_REQUEST_RECEIVED,
            sender = NotificationPersonInfo("sender@test.com", "sender"),
            receiver = NotificationPersonInfo("receiver@test.com", "receiver")
        )

        val message = NotificationQueueMessage.from(request, mapOf("extra" to "data"))

        assertThat(message.category).isEqualTo(request.category)
        assertThat(message.sender).isEqualTo(request.sender)
        assertThat(message.receiver).isEqualTo(request.receiver)
        assertThat(message.data).containsEntry("extra", "data")
        assertThat(message.timestamp).isNotNull
        assertThat(message.messageId).isNotNull
    }

    @Test
    @DisplayName("NotificationSendRequest로부터 QueueMessage를 생성한다 (데이터 없음)")
    fun from_noData() {
        val request = NotificationSendRequest(
            category = NotificationCategory.FRIEND_REQUEST_ACCEPTED,
            sender = NotificationPersonInfo("sender@test.com", "sender"),
            receiver = NotificationPersonInfo("receiver@test.com", "receiver")
        )

        val message = NotificationQueueMessage.from(request)

        assertThat(message.data).isNull()
    }
}
