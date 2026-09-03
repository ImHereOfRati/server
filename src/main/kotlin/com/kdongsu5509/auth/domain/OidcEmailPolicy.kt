package com.kdongsu5509.auth.domain

import com.kdongsu5509.user.domain.OAuth2Provider

object OidcEmailPolicy {

    private const val FALLBACK_DOMAIN = "noreply.imhere.invalid"
    private val PROVIDERS_WITHOUT_GUARANTEED_EMAIL = setOf(OAuth2Provider.APPLE)
    private val UNSAFE_LOCAL_PART = Regex("[^A-Za-z0-9._-]")

    fun allowsMissingEmail(provider: OAuth2Provider): Boolean =
        provider in PROVIDERS_WITHOUT_GUARANTEED_EMAIL

    fun fallbackEmail(provider: OAuth2Provider, sub: String): String =
        "${provider.name.lowercase()}_${UNSAFE_LOCAL_PART.replace(sub, "_")}@$FALLBACK_DOMAIN"
}
