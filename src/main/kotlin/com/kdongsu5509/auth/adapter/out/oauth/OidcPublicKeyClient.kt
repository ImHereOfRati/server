package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.shared.cache.CachePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
class OidcPublicKeyClient(
    private val restClientBuilder: RestClient.Builder,
    private val cachePort: CachePort
) : OauthClientPort {

    companion object {
        private val CACHE_DURATION = Duration.ofDays(8)
        private val log = LoggerFactory.getLogger(OidcPublicKeyClient::class.java)
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
        val response = runCatching {
            restClientBuilder.build()
                .get()
                .uri(jwksUri)
                .retrieve()
                .body(OIDCPublicKeyResponse::class.java)
        }.getOrElse { exception ->
            log.warn("OIDC 공개키(JWKS) 조회 실패: uri={}", jwksUri, exception)
            return null
        }

        if (response == null) {
            log.warn("OIDC 공개키(JWKS) 응답 본문이 비어 있습니다: uri={}", jwksUri)
            return null
        }
        if (response.keys.isEmpty()) {
            log.warn("OIDC 공개키(JWKS) 응답에 키가 없습니다: uri={}", jwksUri)
        }

        return response
    }
}
