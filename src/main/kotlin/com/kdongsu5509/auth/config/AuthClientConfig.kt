package com.kdongsu5509.auth.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.shared.cache.LocalCacheAdapter
import com.kdongsu5509.auth.adapter.out.oauth.OidcPublicKeyApiClient
import com.kdongsu5509.shared.cache.CachePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
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

    @Bean
    fun oidcPublicKeyApiClient(restClientBuilder: Builder): OidcPublicKeyApiClient {
        val restClient = restClientBuilder
            .baseUrl("https://kauth.kakao.com")
            .build()

        val adapter = RestClientAdapter.create(restClient)
        val httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(adapter)

        return httpServiceProxyFactory.build().createClient<OidcPublicKeyApiClient>()
    }
}
