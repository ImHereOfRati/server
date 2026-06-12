package com.kdongsu5509.auth.adapter.out.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ImHereJwtPropertiesTest {

    @Test
    @DisplayName("기본 생성자로 인스턴스를 생성할 수 있다")
    fun create() {
        val properties = ImHereJwtProperties()
        assertThat(properties.secret).isEmpty()
        assertThat(properties.accessExpirationMinutes).isEqualTo(0)
        assertThat(properties.refreshExpirationDays).isEqualTo(0)
        assertThat(properties.adminExpirationMinutes).isEqualTo(0)
        assertThat(properties.accessHeaderName).isEmpty()
    }

    @Test
    @DisplayName("프로퍼티 값을 설정하고 읽을 수 있다")
    fun propertiesSetGet() {
        val properties = ImHereJwtProperties(
            secret = "my-secret",
            accessExpirationMinutes = 1,
            refreshExpirationDays = 7,
            adminExpirationMinutes = 30,
            accessHeaderName = "Authorization"
        )

        assertThat(properties.secret).isEqualTo("my-secret")
        assertThat(properties.accessExpirationMinutes).isEqualTo(1)
        assertThat(properties.refreshExpirationDays).isEqualTo(7)
        assertThat(properties.adminExpirationMinutes).isEqualTo(30)
        assertThat(properties.accessHeaderName).isEqualTo("Authorization")
    }
}
