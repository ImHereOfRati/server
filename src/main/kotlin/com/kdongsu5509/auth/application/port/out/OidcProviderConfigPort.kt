package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.user.domain.OAuth2Provider

interface OidcProviderConfigPort {
    fun get(provider: OAuth2Provider): OidcProviderConfig
    fun configuredProviders(): List<OAuth2Provider>
}

/**
 * application이 소유하는 provider 설정 값객체. (adapter 바인딩 타입과 분리)
 */
data class OidcProviderConfig(
    val issuer: String,
    val audience: String,
    val cacheKey: String,
    val jwksUri: String,
)
