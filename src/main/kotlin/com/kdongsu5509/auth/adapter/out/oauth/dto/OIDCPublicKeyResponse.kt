package com.kdongsu5509.auth.adapter.out.oauth.dto

data class OIDCPublicKeyResponse(
    val keys: List<OIDCPublicKey> = emptyList()
)
