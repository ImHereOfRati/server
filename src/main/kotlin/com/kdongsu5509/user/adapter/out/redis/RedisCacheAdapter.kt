package com.kdongsu5509.user.adapter.out.redis

import com.kdongsu5509.user.application.port.out.user.CachePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Service
class RedisCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    private val mapper: JsonMapper
) : CachePort {

    override fun save(key: String, data: Any, duration: Duration) {
        val jsonString = mapper.writeValueAsString(data)
        redisTemplate.opsForValue().set(key, jsonString, duration)
    }

    override fun <T> find(key: String, clazz: Class<T>): T? {
        val jsonString = redisTemplate.opsForValue().get(key) ?: return null

        return try {
            mapper.readValue(jsonString, clazz)
        } catch (e: Exception) {
            null
        }
    }

    override fun delete(key: String) {
        redisTemplate.delete(key)
    }
}
