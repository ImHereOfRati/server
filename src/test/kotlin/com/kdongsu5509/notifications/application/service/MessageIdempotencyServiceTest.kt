package com.kdongsu5509.notifications.application.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class MessageIdempotencyServiceTest {

    @Mock
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var service: MessageIdempotencyService

    @BeforeEach
    fun setUp() {
        service = MessageIdempotencyService(stringRedisTemplate)
    }

    @Test
    @DisplayName("메시지가 처리되었는지 확인한다 - 이미 처리됨")
    fun isAlreadyProcessed_true() {
        whenever(stringRedisTemplate.hasKey("msg:processed:msg-123")).thenReturn(true)

        val result = service.isAlreadyProcessed("msg-123")

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("메시지가 처리되었는지 확인한다 - 처리되지 않음")
    fun isAlreadyProcessed_false() {
        whenever(stringRedisTemplate.hasKey("msg:processed:msg-123")).thenReturn(false)

        val result = service.isAlreadyProcessed("msg-123")

        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("메시지를 처리됨으로 표시한다")
    fun markAsProcessed() {
        whenever(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)

        service.markAsProcessed("msg-123")

        verify(valueOperations).set("msg:processed:msg-123", "1", Duration.ofHours(48))
    }
}
