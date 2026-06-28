package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationCommandTest {

    private fun command(extraData: Map<String, String>) = NotificationCommand(
        senderNickname = "sender",
        senderEmail = "sender@example.com",
        notificationMethod = NotificationMethod.SMS,
        targetIdentifier = "01012345678",
        type = "TYPE",
        extraData = extraData,
    )

    @Test
    @DisplayName("body 접근자는 extraData의 body 값을 반환한다")
    fun body_present() {
        assertThat(command(mapOf("body" to "hello")).body).isEqualTo("hello")
    }

    @Test
    @DisplayName("body가 없거나 공백이면 null을 반환한다")
    fun body_absent_or_blank() {
        assertThat(command(emptyMap()).body).isNull()
        assertThat(command(mapOf("body" to "   ")).body).isNull()
    }
}
