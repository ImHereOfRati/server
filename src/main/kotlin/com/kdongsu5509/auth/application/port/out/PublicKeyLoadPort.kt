package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.domain.OAuth2Provider

interface PublicKeyLoadPort {
    fun findByKeyId(provider: OAuth2Provider, kid: String): OIDCPublicKey
}
