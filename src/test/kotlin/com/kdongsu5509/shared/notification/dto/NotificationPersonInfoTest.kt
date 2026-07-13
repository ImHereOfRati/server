package com.kdongsu5509.shared.notification.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationPersonInfoTest {

    @Test
    @DisplayName("유효한 이메일이면 생성한다")
    fun accepts_validEmail() {
        val info = NotificationPersonInfo("user@test.com", "nick")

        assertThat(info.email).isEqualTo("user@test.com")
    }

    @Test
    @DisplayName("빈 이메일은 거부한다")
    fun rejects_blankEmail() {
        assertThatThrownBy { NotificationPersonInfo(" ", "nick") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("@가 없는 이메일은 거부한다")
    fun rejects_malformedEmail() {
        assertThatThrownBy { NotificationPersonInfo("not-an-email", "nick") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
