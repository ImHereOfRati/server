package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.ImHereBaseException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class SmsDailyRecipientRateLimiterTest {
    private val limiter = SmsDailyRecipientRateLimiter()

    @Test
    fun `발신자별로 하루 신규 수신자 4명까지만 예약한다`() {
        val senderId = UUID.randomUUID()

        limiter.reserve(senderId, listOf("010-0000-0001", "01000000002"))
        limiter.reserve(senderId, listOf("010-0000-0003", "01000000004"))

        assertThatThrownBy { limiter.reserve(senderId, listOf("010-0000-0005")) }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting { (it as ImHereBaseException).errorCode }
            .isEqualTo(NotificationException.SMS_DAILY_RECIPIENT_LIMIT)
    }

    @Test
    fun `전화번호 표기만 다른 동일 수신자는 중복으로 차감하지 않는다`() {
        val senderId = UUID.randomUUID()

        limiter.reserve(senderId, listOf("010-0000-0011", "01000000011"))
        limiter.reserve(senderId, listOf("010-0000-0012", "010-0000-0013", "010-0000-0014"))

        assertThatThrownBy { limiter.reserve(senderId, listOf("010-0000-0015")) }
            .isInstanceOf(ImHereBaseException::class.java)
    }

    @Test
    fun `이미 예약된 수신자로 재요청해도 예외 없이 통과하고 한도를 추가로 소모하지 않는다`() {
        val senderId = UUID.randomUUID()

        limiter.reserve(senderId, listOf("010-0000-0031"))
        limiter.reserve(senderId, listOf("010-0000-0031"))
        limiter.reserve(senderId, listOf("010-0000-0031"))

        limiter.reserve(senderId, listOf("010-0000-0032", "010-0000-0033", "010-0000-0034"))
        assertThatThrownBy { limiter.reserve(senderId, listOf("010-0000-0035")) }
            .isInstanceOf(ImHereBaseException::class.java)
    }

    @Test
    fun `한 요청이 남은 한도를 초과하면 일부 수신자도 예약하지 않는다`() {
        val senderId = UUID.randomUUID()

        limiter.reserve(senderId, listOf("010-0000-0021", "010-0000-0022", "010-0000-0023"))
        assertThatThrownBy {
            limiter.reserve(senderId, listOf("010-0000-0024", "010-0000-0025"))
        }.isInstanceOf(ImHereBaseException::class.java)

        limiter.reserve(senderId, listOf("010-0000-0024"))
    }
}
