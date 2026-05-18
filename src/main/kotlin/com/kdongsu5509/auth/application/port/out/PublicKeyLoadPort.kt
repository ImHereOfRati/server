package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey

interface PublicKeyLoadPort {
    fun findByKeyId(kid: String): OIDCPublicKey
}
