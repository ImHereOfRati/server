package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.out.OauthClientPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OauthPublicKeyServiceTest {

    @Mock
    private lateinit var oauthClientPort: OauthClientPort

    private lateinit var publicKeyService: OauthPublicKeyService

    @BeforeEach
    fun setUp() {
        publicKeyService = OauthPublicKeyService(oauthClientPort)
    }

    @Test
    @DisplayName("OAuth 공개키를 외부 API로부터 성공적으로 강제 갱신한다")
    fun fetch_success() {
        // when
        publicKeyService.fetch()

        // then
        then(oauthClientPort).should().refresh()
    }
}
