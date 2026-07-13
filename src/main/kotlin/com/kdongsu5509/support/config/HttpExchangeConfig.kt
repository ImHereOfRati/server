package com.kdongsu5509.support.config

import com.kdongsu5509.support.external.DiscordApiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient.Builder
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

/**
 * support 소유의 HTTP 클라이언트 빈(Discord)만 제공한다.
 * auth의 OIDC 클라이언트 조립은 auth 모듈(AuthClientConfig)이 소유한다(support→auth 결합 제거).
 */
@Configuration
class HttpExchangeConfig {

    @Bean
    fun discordApiClient(restClientBuilder: Builder): DiscordApiClient {
        val httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(
            RestClientAdapter.create(
                restClientBuilder.build()
            )
        )

        return httpServiceProxyFactory.build().createClient<DiscordApiClient>()
    }
}
