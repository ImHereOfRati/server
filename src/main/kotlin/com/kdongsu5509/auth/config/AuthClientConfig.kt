package com.kdongsu5509.auth.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.auth.adapter.out.cache.LocalCacheAdapter
import com.kdongsu5509.auth.adapter.out.oauth.OidcPublicKeyApiClient
import com.kdongsu5509.auth.application.port.out.CachePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import tools.jackson.databind.json.JsonMapper
import java.time.Clock

/**
 * auth 모듈의 out 어댑터(캐시·OIDC) 조립을 auth 자신이 소유한다.
 *
 * 과거에는 support.config가 auth의 adapter/out·port를 직접 배선해 support→auth 결합이 있었다.
 * Composition Root를 각 모듈이 소유하도록 이 배선을 auth로 회수한다. support는 프레임워크 기술(RestClient.Builder/Caffeine/JsonMapper)만 제공한다.
 * (외부 연동 방식·빈 시그니처는 불변 — 정의 위치만 이동.)
 */
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
