package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.user.domain.OAuth2Provider

interface OidcProviderConfigPort {
    fun get(provider: OAuth2Provider): OidcProviderConfig
    fun configuredProviders(): List<OAuth2Provider>
}

data class OidcProviderConfig(
    val issuers: List<String>,
    val audiences: List<String>,
    val cacheKey: String,
    val jwksUri: String,
)
