package com.kdongsu5509.auth.adapter.out.oauth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class KakaoOIDCPropertiesTest {

    @Test
    @DisplayName("기본 생성자로 인스턴스를 생성할 수 있다")
    fun create() {
        val properties = KakaoOIDCProperties()
        assertThat(properties.issuer).isEmpty()
        assertThat(properties.audience).isEmpty()
        assertThat(properties.cacheKey).isEmpty()
    }

    @Test
    @DisplayName("프로퍼티 값을 설정하고 읽을 수 있다")
    fun propertiesSetGet() {
        val properties = KakaoOIDCProperties(
            issuer = "test-issuer",
            audience = "test-aud",
            cacheKey = "test-key"
        )
        
        assertThat(properties.issuer).isEqualTo("test-issuer")
        assertThat(properties.audience).isEqualTo("test-aud")
        assertThat(properties.cacheKey).isEqualTo("test-key")
    }
}
