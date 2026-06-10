package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.CachePort
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class KakaoOauthClient(
    private val kakaoOauthPublicKeyApiClient: KakaoOauthPublicKeyApiClient,
    private val cachePort: CachePort
) : OauthClientPort {

    companion object {
        private const val CACHE_KEY = "kakaoOidcKeys::kakaoPublicKeySet"
        private val CACHE_DURATION = Duration.ofDays(8)
    }

    override fun fetch(): OIDCPublicKeyResponse? {
        val cached = cachePort.find(CACHE_KEY, OIDCPublicKeyResponse::class.java)
        if (cached != null) {
            return cached
        }
        val fetched = kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey()
        if (fetched != null) {
            cachePort.save(CACHE_KEY, fetched, CACHE_DURATION)
        }
        return fetched
    }

    override fun refresh(): OIDCPublicKeyResponse? {
        val fetched = kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey()
        if (fetched != null) {
            cachePort.save(CACHE_KEY, fetched, CACHE_DURATION)
        }
        return fetched
    }
}
