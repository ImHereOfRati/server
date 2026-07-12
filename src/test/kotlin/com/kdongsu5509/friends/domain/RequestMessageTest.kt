package com.kdongsu5509.friends.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RequestMessageTest {

    @Test
    @DisplayName("빈 값이 아니면 정상 생성된다")
    fun success() {
        val message = RequestMessage("hi")

        assertThat(message.value).isEqualTo("hi")
    }

    @Test
    @DisplayName("빈 값이면 예외를 발생시킨다")
    fun blank_throws() {
        assertThrows<IllegalArgumentException> {
            RequestMessage("   ")
        }
    }
}
