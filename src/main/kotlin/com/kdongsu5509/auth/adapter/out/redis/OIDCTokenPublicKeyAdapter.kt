package com.kdongsu5509.auth.adapter.out.redis

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Component

@Component
class OIDCTokenPublicKeyAdapter(private val oauthClientPort: OauthClientPort) : PublicKeyLoadPort {

    override fun findByKeyId(kid: String): OIDCPublicKey {
        val cachedKeys = oauthClientPort.fetch()
            ?: AuthException.KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED.throwIt()

        cachedKeys.keys.firstOrNull { it.kid == kid }?.let { return it }

        return findAfterRefresh(kid)
    }

    private fun findAfterRefresh(kid: String): OIDCPublicKey {
        val refreshedKeys = oauthClientPort.refresh()
            ?: AuthException.KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED.throwIt()

        return refreshedKeys.keys.firstOrNull { it.kid == kid }
            ?: AuthException.KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND.throwIt()
    }
}
