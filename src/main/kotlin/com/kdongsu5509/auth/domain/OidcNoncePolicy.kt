package com.kdongsu5509.auth.domain

import java.security.MessageDigest
import java.util.*

object OidcNoncePolicy {

    fun matches(actualNonce: String?, expectedNonce: String): Boolean {
        if (actualNonce.isNullOrBlank() || expectedNonce.isBlank()) {
            return false
        }
        if (actualNonce == expectedNonce) {
            return true
        }
        return actualNonce.lowercase(Locale.ROOT) == sha256Hex(expectedNonce)
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
}
