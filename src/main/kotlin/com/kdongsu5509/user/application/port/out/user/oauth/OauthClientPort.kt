package com.kdongsu5509.user.application.port.out.user.oauth

import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse


interface OauthClientPort {
    fun fetchPublicKey(): OIDCPublicKeyResponse?
}
