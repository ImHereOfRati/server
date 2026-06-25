package com.kdongsu5509.support.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.auth.adapter.out.cache.LocalCacheAdapter
import com.kdongsu5509.auth.application.port.out.CachePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Clock

@Configuration
class LocalCacheConfig {

    @Bean
    fun jsonMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()
    }

    @Bean
    fun cacheClock(): Clock = Clock.systemUTC()

    @Bean
    fun localCacheAdapter(
        jsonMapper: JsonMapper,
        cacheClock: Clock
    ): CachePort {
        return LocalCacheAdapter(
            caffeine = Caffeine.newBuilder().maximumSize(10_000),
            jsonMapper = jsonMapper,
            clock = cacheClock
        )
    }
}
