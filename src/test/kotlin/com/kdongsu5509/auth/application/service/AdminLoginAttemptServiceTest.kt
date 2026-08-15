package com.kdongsu5509.auth.application.service

import com.kdongsu5509.shared.cache.CachePort
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class AdminLoginAttemptServiceTest {

    private val cache = FakeCache()
    private val service = AdminLoginAttemptService(cache)

    @Test
    fun `four failed attempts are not blocked`() {
        repeat(4) { service.recordFailure("admin", "127.0.0.1") }

        service.ensureNotBlocked("admin", "127.0.0.1")

        assertThat(cache.saved.last().third).isEqualTo(Duration.ofDays(7))
    }

    @Test
    fun `fifth failed attempt blocks the key for seven days`() {
        repeat(5) { service.recordFailure("admin", "127.0.0.1") }

        assertThatThrownBy { service.ensureNotBlocked("admin", "127.0.0.1") }
            .isInstanceOf(AdminLoginBlockedException::class.java)
    }

    @Test
    fun `successful password authentication clears the failure state`() {
        repeat(5) { service.recordFailure("admin", "127.0.0.1") }
        service.recordSuccess("admin", "127.0.0.1")

        service.ensureNotBlocked("admin", "127.0.0.1")
        assertThat(cache.values).isEmpty()
    }

    private class FakeCache : CachePort {
        val values = mutableMapOf<String, Any>()
        val saved = mutableListOf<Triple<String, Any, Duration>>()

        override fun save(key: String, data: Any, duration: Duration) {
            values[key] = data
            saved += Triple(key, data, duration)
        }

        override fun <T> find(key: String, clazz: Class<T>): T? = values[key] as? T

        override fun delete(key: String) { values.remove(key) }

        override fun replace(key: String, expected: Any, replacement: Any, duration: Duration): Boolean = false
    }
}
