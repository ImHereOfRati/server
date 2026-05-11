package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.port.out.user.oauth.OauthClientPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OauthPublicKeyService(
    private val oauthClientPort: OauthClientPort
) {
    private val log = LoggerFactory.getLogger(OauthPublicKeyService::class.java)

    fun fetch() {
        log.info("카카오 공개키 요청")
        oauthClientPort.fetchPublicKey()
        log.info("카카오 공개키 수신 완료")
    }
}
