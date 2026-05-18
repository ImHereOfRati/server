package com.kdongsu5509.support.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration


@Configuration
@EnableCaching
@EnableRedisRepositories(basePackages = ["com.kdongsu5509.auth.adapter.out.redis"])
class RedisConfig {

    @Value("\${spring.data.redis.host:localhost}")
    private var host: String? = null

    @Value("\${spring.data.redis.port:6379}")
    private var port: String = "6379"

    @Bean
    fun jsonRedisSerializer(): GenericJacksonJsonRedisSerializer {
        return GenericJacksonJsonRedisSerializer(getJsonMapper())
    }

    private fun getJsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()
    }

    @Bean
    @Primary
    fun redisCacheManager(
        cf: RedisConnectionFactory,
        jsonRedisSerializer: GenericJacksonJsonRedisSerializer
    ): CacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        StringRedisSerializer()
                    )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        jsonRedisSerializer
                    )
            )
            .entryTtl(Duration.ofHours(1L))

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(cf)
            .cacheDefaults(config)
            .build()
    }

    @Bean
    fun oidcCacheManager(
        cf: RedisConnectionFactory,
        jsonRedisSerializer: GenericJacksonJsonRedisSerializer
    ): CacheManager {
        val config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        StringRedisSerializer()
                    )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(
                        jsonRedisSerializer
                    )
            )
            .entryTtl(Duration.ofDays(8L))

        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(cf)
            .cacheDefaults(config)
            .build()
    }

    @Bean("customRedisTemplate")
    fun redisTemplate(
        cf: RedisConnectionFactory,
        jsonRedisSerializer: GenericJacksonJsonRedisSerializer
    ): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            connectionFactory = cf
            keySerializer = StringRedisSerializer()
            valueSerializer = jsonRedisSerializer
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = jsonRedisSerializer
            afterPropertiesSet()
        }
    }
}
