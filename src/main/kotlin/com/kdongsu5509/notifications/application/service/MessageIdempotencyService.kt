package com.kdongsu5509.notifications.application.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MessageIdempotencyService(
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        private const val KEY_PREFIX = "msg:processed:"
        private val TTL = Duration.ofHours(48)
    }

    fun isAlreadyProcessed(messageId: String): Boolean {
        return stringRedisTemplate.hasKey(KEY_PREFIX + messageId) ?: false
    }

    fun markAsProcessed(messageId: String) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + messageId, "1", TTL)
    }
}
