package com.kdongsu5509.auth.application.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times

@ExtendWith(MockitoExtension::class)
class OIDCPublicKeySchedulerTest {

    @Mock
    private lateinit var oauthPublicKeyService: OauthPublicKeyService

    private lateinit var oidcPublicKeyScheduler: OIDCPublicKeyScheduler

    @BeforeEach
    fun setUp() {
        oidcPublicKeyScheduler = OIDCPublicKeyScheduler(oauthPublicKeyService)
    }

    @Test
    @DisplayName("애플리케이션 시작 시(PostConstruct) OIDC 공개키 캐시를 초기화한다")
    fun initializePublicKeyCache_success() {
        // when
        oidcPublicKeyScheduler.initializePublicKeyCache()

        // then
        then(oauthPublicKeyService).should(times(1)).fetchAll()
    }

    @Test
    @DisplayName("스케줄링된 주기에 따라 OIDC 공개키를 업데이트한다")
    fun updatePublicKey_success() {
        // when
        oidcPublicKeyScheduler.updatePublicKey()

        // then
        then(oauthPublicKeyService).should(times(1)).fetchAll()
    }
}
