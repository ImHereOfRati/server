package com.kdongsu5509.auth.adapter.out.oauth.dto

data class OIDCPublicKey(
    val kid: String = "",
    val kty: String = "",
    val alg: String = "",
    val use: String = "",
    val n: String = "", // modulus
    val e: String = ""  // exponent
)
