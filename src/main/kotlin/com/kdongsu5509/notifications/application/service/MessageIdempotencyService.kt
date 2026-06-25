package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.auth.application.port.out.CachePort
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class MessageIdempotencyService(
    private val cachePort: CachePort
) {
    companion object {
        private const val KEY_PREFIX = "msg:processed:"
        private val TTL = Duration.ofHours(48)
    }

    fun isAlreadyProcessed(messageId: String): Boolean {
        return cachePort.find(KEY_PREFIX + messageId, String::class.java) != null
    }

    fun markAsProcessed(messageId: String) {
        cachePort.save(KEY_PREFIX + messageId, "1", TTL)
    }
}
