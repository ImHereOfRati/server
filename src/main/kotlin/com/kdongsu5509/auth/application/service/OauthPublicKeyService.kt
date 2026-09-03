package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.auth.application.port.out.OidcProviderConfigPort
import com.kdongsu5509.user.domain.OAuth2Provider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OauthPublicKeyService(
    private val oauthClientPort: OauthClientPort,
    private val providerConfigPort: OidcProviderConfigPort
) {
    private val log = LoggerFactory.getLogger(OauthPublicKeyService::class.java)

    fun fetch(provider: OAuth2Provider) {
        val providerProperties = providerConfigPort.get(provider)

        log.info("OIDC 공개키 강제 갱신 요청: {}", provider)
        val refreshed = oauthClientPort.refresh(providerProperties.cacheKey, providerProperties.jwksUri)
        if (refreshed == null) {
            log.warn("OIDC 공개키 강제 갱신 실패: provider={}, jwksUri={}", provider, providerProperties.jwksUri)
            return
        }
        log.info("OIDC 공개키 강제 갱신 완료: provider={}, keyCount={}", provider, refreshed.keys.size)
    }

    fun fetchAll() {
        providerConfigPort.configuredProviders().forEach { provider ->
            runCatching { fetch(provider) }
                .onFailure { exception -> log.warn("OIDC 공개키 갱신 중 오류: provider={}", provider, exception) }
        }
    }
}
