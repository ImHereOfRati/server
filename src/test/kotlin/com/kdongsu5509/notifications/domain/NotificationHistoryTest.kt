package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationHistoryTest {

    @Test
    @DisplayName("markAsRead를 호출하면 isRead가 true인 새로운 불변 객체가 반환된다")
    fun markAsRead() {
        // given
        val notification = NotificationHistory(
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "NOTICE",
            path = null,
            isRead = false
        )

        // when
        val updatedNotification = notification.markAsRead()

        // then
        assertThat(notification.isRead).isFalse() // 원본은 변경되지 않음
        assertThat(updatedNotification.isRead).isTrue() // 새로운 객체는 읽음 처리됨
        assertThat(updatedNotification).isNotSameAs(notification) // 서로 다른 인스턴스
        
        // 나머지 필드는 그대로 유지되어야 함
        assertThat(updatedNotification.receiverEmail).isEqualTo(notification.receiverEmail)
        assertThat(updatedNotification.senderNickname).isEqualTo(notification.senderNickname)
        assertThat(updatedNotification.title).isEqualTo(notification.title)
    }
}
