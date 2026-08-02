package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.shared.cache.CachePort
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.Duration

@Component
class OidcPublicKeyClient(
    private val oidcPublicKeyApiClient: OidcPublicKeyApiClient,
    private val restClientBuilder: RestClient.Builder,
    private val cachePort: CachePort
) : OauthClientPort {

    companion object {
        private val CACHE_DURATION = Duration.ofDays(8)

        // Kakao JWKS는 전용 API 클라이언트로 조회한다. jwksUri에 provider 정보가
        // 실려있지 않아 호스트로 식별한다. provider를 명시적으로 넘기려면 OauthClientPort.fetch
        // 시그니처를 바꿔야 해(경계 제약) 보류 — 우선 호스트 문자열만 상수로 명시한다.
        private const val KAKAO_JWKS_HOST = "kauth.kakao.com"
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
        if (jwksUri.contains(KAKAO_JWKS_HOST)) {
            return runCatching { oidcPublicKeyApiClient.fetchKakaoPublicKey() }.getOrNull()
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
