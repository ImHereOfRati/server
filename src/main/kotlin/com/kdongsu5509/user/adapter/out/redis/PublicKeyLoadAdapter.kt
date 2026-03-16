package com.kdongsu5509.user.adapter.out.redis

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import org.springframework.stereotype.Component

@Component
class PublicKeyLoadAdapter(
    private val cachePort: CachePort,
    private val kakaoOIDCProperties: KakaoOIDCProperties
) : PublicKeyLoadPort {
    override fun loadPublicKey(kid: String): OIDCPublicKey {
        val cachedPublicKeys = getCachedPublicKeys()

        val oidcPublicKey: OIDCPublicKey =
            cachedPublicKeys.keys.firstOrNull { it.kid == kid }
                ?: throw BusinessException(AuthErrorCode.KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND)

        return oidcPublicKey
    }

    private fun getCachedPublicKeys(): OIDCPublicKeyResponse {
        val cachedKeySet = cachePort.find(kakaoOIDCProperties.cacheKey) as? OIDCPublicKeyResponse
            ?: throw BusinessException(AuthErrorCode.KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED)
        return cachedKeySet
    }
}