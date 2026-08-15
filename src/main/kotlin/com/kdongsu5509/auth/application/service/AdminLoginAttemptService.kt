package com.kdongsu5509.auth.application.service

import com.kdongsu5509.shared.cache.CachePort
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class AdminLoginAttemptService(
    private val cache: CachePort,
) {
    fun ensureNotBlocked(adminId: String, clientIp: String) {
        val record = find(adminId, clientIp) ?: return
        if (record.blockedUntilEpochSeconds > Instant.now().epochSecond) {
            throw AdminLoginBlockedException(Instant.ofEpochSecond(record.blockedUntilEpochSeconds))
        }
    }

    fun recordFailure(adminId: String, clientIp: String) {
        val attempts = (find(adminId, clientIp)?.failedAttempts ?: 0) + 1
        val blockedUntil = if (attempts >= MAX_FAILURES) {
            Instant.now().plus(Duration.ofDays(BLOCK_DAYS)).epochSecond
        } else {
            0L
        }
        cache.save(key(adminId, clientIp), LoginAttempt(attempts, blockedUntil), Duration.ofDays(BLOCK_DAYS))
    }

    fun recordSuccess(adminId: String, clientIp: String) {
        cache.delete(key(adminId, clientIp))
    }

    private fun find(adminId: String, clientIp: String): LoginAttempt? =
        cache.find(key(adminId, clientIp), LoginAttempt::class.java)

    private fun key(adminId: String, clientIp: String) =
        "admin-login-attempt:${normalize(adminId)}:${normalize(clientIp)}"

    private fun normalize(value: String): String = value.trim().ifEmpty { "unknown" }

    companion object {
        const val MAX_FAILURES = 5
        const val BLOCK_DAYS = 7L
    }
}

data class LoginAttempt(
    val failedAttempts: Int = 0,
    val blockedUntilEpochSeconds: Long = 0,
)

class AdminLoginBlockedException(val blockedUntil: Instant) : RuntimeException("Too many login attempts")
