package com.kdongsu5509.auth.adapter.out.redis

import com.kdongsu5509.auth.application.port.out.CachePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Component
class AuthRedisCacheAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    private val mapper: JsonMapper
) : CachePort {

    override fun save(key: String, data: Any, duration: Duration) {
        val jsonString = mapper.writeValueAsString(data)
        redisTemplate.opsForValue().set(key, jsonString, duration)
    }

    override fun <T> find(key: String, clazz: Class<T>): T? {
        val jsonString = redisTemplate.opsForValue().get(key)
        return mapper.readValue(jsonString, clazz)
    }

    override fun delete(key: String) {
        redisTemplate.delete(key)
    }
}
