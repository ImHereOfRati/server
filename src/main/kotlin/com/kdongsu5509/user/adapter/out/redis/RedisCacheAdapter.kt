package com.kdongsu5509.user.adapter.out.redis

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.exception.AuthError
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@Component
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
            AuthError.OIDC_KEY_PARSING_ERROR.throwIt()
        }
    }

    override fun delete(key: String) {
        redisTemplate.delete(key)
    }
}
