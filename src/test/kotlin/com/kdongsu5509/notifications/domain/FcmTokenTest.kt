package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FcmTokenTest {

    @Test
    @DisplayName("create 시 이메일 또는 토큰이 공백이면 실패한다")
    fun create_blank_fails() {
        assertThatThrownBy { FcmToken.create(email = "", fcmToken = "t", deviceType = DeviceType.AOS) }
            .isInstanceOf(InvalidInputException::class.java)
        assertThatThrownBy { FcmToken.create(email = "a@b.com", fcmToken = " ", deviceType = DeviceType.AOS) }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("update 시 토큰이 공백이면 실패한다")
    fun update_blank_fails() {
        val token = FcmToken.create(email = "a@b.com", fcmToken = "old", deviceType = DeviceType.AOS)
        assertThatThrownBy { token.update("  ") }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("update 메서드를 호출하면 fcmToken 값만 새로운 값으로 변경된 객체를 반환한다")
    fun update_changes_only_fcm_token() {
        // given
        val newTokenValue = "new-token-value-123"


        val originalToken = FcmToken(
            id = 1L,
            email = "test@example.com",
            fcmToken = "old-token-value",
            deviceType = DeviceType.AOS,
            createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        )

        val expect = FcmToken(
            id = 1L,
            email = "test@example.com",
            fcmToken = newTokenValue,
            deviceType = DeviceType.AOS,
            createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        )


        // when
        val updatedToken = originalToken.update(newTokenValue)

        // then
        assertThat(updatedToken).isEqualTo(expect)
        assertThat(originalToken.fcmToken).isEqualTo("old-token-value")
    }
}
