package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.CachePort
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import org.springframework.web.client.RestClient
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KakaoOauthClient(
    private val kakaoOauthPublicKeyApiClient: KakaoOauthPublicKeyApiClient,
    private val restClientBuilder: RestClient.Builder,
    private val cachePort: CachePort
) : OauthClientPort {

    companion object {
        private val CACHE_DURATION = Duration.ofDays(8)
    }

    override fun fetch(cacheKey: String, jwksUri: String): OIDCPublicKeyResponse? {
        val cached = cachePort.find(cacheKey, OIDCPublicKeyResponse::class.java)
        if (cached != null) {
            return cached
        }
        val fetched = fetchRemote(jwksUri)
        if (fetched != null) {
            cachePort.save(cacheKey, fetched, CACHE_DURATION)
        }
        return fetched
    }

    override fun refresh(cacheKey: String, jwksUri: String): OIDCPublicKeyResponse? {
        val fetched = fetchRemote(jwksUri)
        if (fetched != null) {
            cachePort.save(cacheKey, fetched, CACHE_DURATION)
        }
        return fetched
    }

    private fun fetchRemote(jwksUri: String): OIDCPublicKeyResponse? {
        if (jwksUri.contains("kauth.kakao.com")) {
            return runCatching { kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey() }.getOrNull()
        }

        return runCatching {
            restClientBuilder.build()
                .get()
                .uri(jwksUri)
                .retrieve()
                .body(OIDCPublicKeyResponse::class.java)
        }.getOrNull()
    }
}
