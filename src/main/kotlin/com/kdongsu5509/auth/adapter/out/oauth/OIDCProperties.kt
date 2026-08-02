package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.application.port.out.OidcProviderConfig
import com.kdongsu5509.auth.application.port.out.OidcProviderConfigPort
import com.kdongsu5509.user.domain.OAuth2Provider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "oidc")
data class OIDCProperties(
    var providers: MutableMap<String, Provider> = mutableMapOf()
) : OidcProviderConfigPort {
    data class Provider(
        var issuers: MutableList<String> = mutableListOf(),
        var audiences: MutableList<String> = mutableListOf(),
        var cacheKey: String = "",
        var jwksUri: String = ""
    )

    override fun get(provider: OAuth2Provider): OidcProviderConfig =
        providers[provider.configKey()]?.toConfig() ?: error("Missing OIDC provider config: $provider")

    override fun configuredProviders(): List<OAuth2Provider> =
        OAuth2Provider.entries.filter { providers.containsKey(it.configKey()) }

    private fun Provider.toConfig(): OidcProviderConfig =
        OidcProviderConfig(
            issuers = issuers.filter { it.isNotBlank() },
            audiences = audiences.filter { it.isNotBlank() },
            cacheKey = cacheKey,
            jwksUri = jwksUri
        )

    private fun OAuth2Provider.configKey(): String = name.lowercase()
}
