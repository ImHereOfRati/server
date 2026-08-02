package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class FcmTokenTest {

    private val ownerId: UUID = UUID.fromString("00000000-0000-0000-0000-0000000000a1")

    @Test
    @DisplayName("생성 시 토큰이 공백이면 실패한다")
    fun create_blank_fails() {
        assertThatThrownBy { FcmToken(ownerId = ownerId, fcmToken = " ", deviceType = DeviceType.AOS) }
            .isInstanceOf(InvalidInputException::class.java)
        assertThatThrownBy { FcmToken(ownerId = ownerId, fcmToken = "", deviceType = DeviceType.AOS) }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("복원 경로도 같은 검증을 거친다")
    fun reconstruct_blank_fails() {
        assertThatThrownBy {
            FcmToken(
                id = 1L,
                ownerId = ownerId,
                fcmToken = "",
                deviceType = DeviceType.AOS,
                createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0)
            )
        }.isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("copy로도 불변식을 우회할 수 없다")
    fun copy_blank_fails() {
        val token = FcmToken(ownerId = ownerId, fcmToken = "old", deviceType = DeviceType.AOS)

        assertThatThrownBy { token.copy(fcmToken = " ") }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("update 시 토큰이 공백이면 실패한다")
    fun update_blank_fails() {
        val token = FcmToken(ownerId = ownerId, fcmToken = "old", deviceType = DeviceType.AOS)
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
            ownerId = ownerId,
            fcmToken = "old-token-value",
            deviceType = DeviceType.AOS,
            createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
            updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0)
        )

        val expect = FcmToken(
            id = 1L,
            ownerId = ownerId,
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
