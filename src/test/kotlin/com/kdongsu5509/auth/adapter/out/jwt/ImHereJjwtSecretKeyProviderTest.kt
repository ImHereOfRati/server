package com.kdongsu5509.auth.adapter.out.jwt

import io.jsonwebtoken.security.Keys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.charset.StandardCharsets

@ExtendWith(MockitoExtension::class)
class ImHereJjwtSecretKeyProviderTest {

    @Mock
    private lateinit var imHereJwtProperties: ImHereJwtProperties

    @InjectMocks
    private lateinit var imHereJjwtSecretKeyProvider: ImHereJjwtSecretKeyProvider

    @Test
    @DisplayName("설정된 시크릿 문자열을 바탕으로 올바른 SecretKey 객체를 생성한다")
    fun secretKey_generation_success() {
        // given
        val testSecret = "testSecretKeyForJwtAuthenticationTesting12345678901234567890"
        `when`(imHereJwtProperties.secret).thenReturn(testSecret)

        val expectedKey = Keys.hmacShaKeyFor(testSecret.toByteArray(StandardCharsets.UTF_8))

        // when
        val actualKey = imHereJjwtSecretKeyProvider.secretKey

        // then
        assertThat(actualKey).isNotNull
        assertThat(actualKey.algorithm).isEqualTo(expectedKey.algorithm)
        assertThat(actualKey.encoded).isEqualTo(expectedKey.encoded)
    }

    @Test
    @DisplayName("secretKey는 지연 로딩(lazy) 방식으로 한 번만 생성된다")
    fun secretKey_isLazyAndCached() {
        // given
        val testSecret = "testSecretKeyForJwtAuthenticationTesting12345678901234567890"
        `when`(imHereJwtProperties.secret).thenReturn(testSecret)

        // when
        val firstCall = imHereJjwtSecretKeyProvider.secretKey
        val secondCall = imHereJjwtSecretKeyProvider.secretKey

        // then
        assertThat(firstCall).isSameAs(secondCall)
    }
}
