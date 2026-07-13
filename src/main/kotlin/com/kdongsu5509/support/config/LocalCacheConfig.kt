package com.kdongsu5509.support.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Clock

/**
 * 프레임워크 공통 기술 빈(JsonMapper, Clock)만 제공한다.
 * auth의 캐시 어댑터 조립은 auth 모듈(AuthClientConfig)이 소유한다(support→auth 결합 제거).
 */
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
}
