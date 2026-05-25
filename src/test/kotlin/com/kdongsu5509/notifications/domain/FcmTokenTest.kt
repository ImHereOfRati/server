package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FcmTokenTest {

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
