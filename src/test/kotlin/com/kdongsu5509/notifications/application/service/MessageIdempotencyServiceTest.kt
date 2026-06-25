package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.auth.application.port.out.CachePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class MessageIdempotencyServiceTest {

    @Mock
    private lateinit var cachePort: CachePort

    private lateinit var service: MessageIdempotencyService

    @BeforeEach
    fun setUp() {
        service = MessageIdempotencyService(cachePort)
    }

    @Test
    @DisplayName("메시지가 처리되었는지 확인한다 - 이미 처리됨")
    fun isAlreadyProcessed_true() {
        whenever(cachePort.find("msg:processed:msg-123", String::class.java)).thenReturn("1")

        val result = service.isAlreadyProcessed("msg-123")

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("메시지가 처리되었는지 확인한다 - 처리되지 않음")
    fun isAlreadyProcessed_false() {
        whenever(cachePort.find("msg:processed:msg-123", String::class.java)).thenReturn(null)

        val result = service.isAlreadyProcessed("msg-123")

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("메시지를 처리됨으로 표시한다")
    fun markAsProcessed() {
        service.markAsProcessed("msg-123")

        verify(cachePort).save("msg:processed:msg-123", "1", Duration.ofHours(48))
    }
}
