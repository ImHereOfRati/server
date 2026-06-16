package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SMSTest {

    @Test
    @DisplayName("SMS 본문은 고정 템플릿으로 렌더링된다")
    fun buildMessageText_success() {
        val sms = SMS(
            senderNickname = "sender",
            receiverNumber = "01012345678",
            location = "Seoul"
        )

        assertThat(sms.buildMessageText()).isEqualTo("[ImHere]\nSeoul 도착\n발신자: sender")
    }

    @Test
    @DisplayName("SMS 본문이 45자를 초과하면 생성 시 실패한다")
    fun buildMessageText_tooLong() {
        assertThatThrownBy {
            SMS(
                senderNickname = "very-long-sender-name",
                receiverNumber = "01012345678",
                location = "very-long-location-name-that-breaks-limit"
            )
        }
            .isInstanceOf(com.kdongsu5509.support.exception.type.InvalidInputException::class.java)
            .hasMessageContaining("45")
    }

    @Test
    @DisplayName("SMS 필수 값이 비면 생성 시 실패한다")
    fun buildMessageText_emptyField() {
        assertThatThrownBy {
            SMS(
                senderNickname = "",
                receiverNumber = "01012345678",
                location = "Seoul"
            )
        }
            .isInstanceOf(com.kdongsu5509.support.exception.type.InvalidInputException::class.java)
            .hasMessageContaining("비어있습니다")
    }
}
