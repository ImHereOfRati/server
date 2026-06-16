package com.kdongsu5509.auth.adapter.out.redis

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Component

@Component
class OIDCTokenPublicKeyAdapter(
    private val oauthClientPort: OauthClientPort,
    private val oidcProperties: OIDCProperties
) : PublicKeyLoadPort {

    override fun findByKeyId(provider: OAuth2Provider, kid: String): OIDCPublicKey {
        val providerProperties = oidcProperties.get(provider)

        val cachedKeys = oauthClientPort.fetch(providerProperties.cacheKey, providerProperties.jwksUri)
            ?: AuthException.OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED.throwIt()

        cachedKeys.keys.firstOrNull { it.kid == kid }?.let { return it }

        return findAfterRefresh(providerProperties.cacheKey, providerProperties.jwksUri, kid)
    }

    private fun findAfterRefresh(cacheKey: String, jwksUri: String, kid: String): OIDCPublicKey {
        val refreshedKeys = oauthClientPort.refresh(cacheKey, jwksUri)
            ?: AuthException.OIDC_PUBLIC_KEY_FETCH_FAILED.throwIt()

        return refreshedKeys.keys.firstOrNull { it.kid == kid }
            ?: AuthException.OIDC_PUBLIC_KEY_NOT_FOUND.throwIt()
    }
}
