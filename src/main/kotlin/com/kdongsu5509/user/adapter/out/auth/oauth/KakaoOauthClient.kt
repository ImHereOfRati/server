package com.kdongsu5509.user.adapter.out.auth.oauth

import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.application.port.out.user.oauth.OauthClientPort
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class KakaoOauthClient(
    private val kakaoOauthPublicKeyApiClient: KakaoOauthPublicKeyApiClient
) : OauthClientPort {

    private val log = LoggerFactory.getLogger(KakaoOauthClient::class.java)

    @Cacheable(value = ["kakaoOidcKeys"], cacheManager = "oidcCacheManager", key = "'kakaoPublicKeySet'")
    override fun fetchPublicKey(): OIDCPublicKeyResponse? {
        return kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey()
    }
}
