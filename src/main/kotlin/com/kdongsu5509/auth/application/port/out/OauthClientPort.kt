package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse


interface OauthClientPort {
    fun fetch(cacheKey: String, jwksUri: String): OIDCPublicKeyResponse?
    fun refresh(cacheKey: String, jwksUri: String): OIDCPublicKeyResponse?
}
