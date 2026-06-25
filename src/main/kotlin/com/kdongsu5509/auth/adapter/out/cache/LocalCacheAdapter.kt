package com.kdongsu5509.auth.adapter.out.cache

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Cache
import com.kdongsu5509.auth.application.port.out.CachePort
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Duration

class LocalCacheAdapter(
    caffeine: Caffeine<Any, Any>,
    private val jsonMapper: JsonMapper,
    private val clock: Clock
) : CachePort {

    private val cache: Cache<String, CacheEntry> = caffeine.build()

    override fun save(key: String, data: Any, duration: Duration) {
        cache.put(
            key,
            CacheEntry(
                serializedValue = jsonMapper.writeValueAsString(data),
                expiresAtMillis = clock.millis() + duration.toMillis()
            )
        )
    }

    override fun <T> find(key: String, clazz: Class<T>): T? {
        val entry = cache.getIfPresent(key) ?: return null
        if (entry.isExpired(clock.millis())) {
            cache.invalidate(key)
            return null
        }

        return jsonMapper.readValue(entry.serializedValue, clazz)
    }

    override fun delete(key: String) {
        cache.invalidate(key)
    }

    private data class CacheEntry(
        val serializedValue: String,
        val expiresAtMillis: Long
    ) {
        fun isExpired(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis
    }
}
