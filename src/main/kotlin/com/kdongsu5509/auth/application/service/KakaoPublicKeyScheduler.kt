package com.kdongsu5509.auth.application.service

import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class KakaoPublicKeyScheduler(
    private val oauthPublicKeyService: OauthPublicKeyService
) {
    companion object {
        private const val DURATION: Long = 7 * 24 * 60 * 60 * 1000 // 7일 (밀리초)
    }

    @PostConstruct
    fun initializePublicKeyCache() {
        oauthPublicKeyService.fetch()
    }

    @Scheduled(fixedRate = DURATION)
    fun updatePublicKey() {
        oauthPublicKeyService.fetch()
    }
}
