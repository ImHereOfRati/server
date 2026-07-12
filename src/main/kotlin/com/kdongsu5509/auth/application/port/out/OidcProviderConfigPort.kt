package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.domain.OAuth2Provider

/**
 * provider별 OIDC 설정을 공급하는 out-port.
 *
 * 이전에는 application 서비스(OIDCVerifyService/OauthPublicKeyService)가
 * 프레임워크 바인딩 세부(adapter의 @ConfigurationProperties 클래스 OIDCProperties)에
 * 직접 결합해 DIP를 위반했다. 서비스는 이 포트에만 의존하고, OIDCProperties가 포트를 구현한다.
 */
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
