package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OauthPublicKeyService(
    private val oauthClientPort: OauthClientPort,
    private val oidcProperties: OIDCProperties
) {
    private val log = LoggerFactory.getLogger(OauthPublicKeyService::class.java)

    fun fetch(provider: OAuth2Provider = OAuth2Provider.KAKAO) {
        val providerProperties = oidcProperties.get(provider)

        log.info("OIDC 공개키 강제 갱신 요청: {}", provider)
        oauthClientPort.refresh(providerProperties.cacheKey, providerProperties.jwksUri)
        log.info("OIDC 공개키 강제 갱신 완료: {}", provider)
    }
}
