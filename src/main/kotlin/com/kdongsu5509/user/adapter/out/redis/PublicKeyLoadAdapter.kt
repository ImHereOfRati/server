package com.kdongsu5509.user.adapter.out.redis

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.application.port.out.user.oauth.OauthClientPort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import com.kdongsu5509.user.exception.AuthError
import org.springframework.stereotype.Component

@Component
class PublicKeyLoadAdapter(private val oauthClientPort: OauthClientPort) : PublicKeyLoadPort {

    override fun loadPublicKey(kid: String): OIDCPublicKey {
        val cachedPublicKeys = getCachedPublicKeys()
        return cachedPublicKeys.keys.firstOrNull { it.kid == kid }
            ?: AuthError.KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND.throwIt()
    }

    private fun getCachedPublicKeys(): OIDCPublicKeyResponse {
        return oauthClientPort.getPublicKeyFromProvider()
            ?: AuthError.KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED.throwIt()
    }
}
