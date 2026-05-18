package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.out.OauthClientPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OauthPublicKeyService(
    private val oauthClientPort: OauthClientPort
) {
    private val log = LoggerFactory.getLogger(OauthPublicKeyService::class.java)

    fun fetch() {
        log.info("카카오 공개키 강제 갱신 요청")
        oauthClientPort.refresh()
        log.info("카카오 공개키 강제 갱신 완료")
    }
}
