package com.kdongsu5509.auth.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.shared.cache.LocalCacheAdapter
import com.kdongsu5509.shared.cache.CachePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

@Configuration
class AuthClientConfig {

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
