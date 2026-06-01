package com.kdongsu5509.auth.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SecurityWhiteListTest {

    @Test
    @DisplayName("기본 생성자로 인스턴스를 생성할 수 있다")
    fun create() {
        val list = SecurityWhiteList()
        assertThat(list.whitelist).isEmpty()
    }

    @Test
    @DisplayName("프로퍼티 값을 설정하고 읽을 수 있다")
    fun propertiesSetGet() {
        val list = SecurityWhiteList(whitelist = listOf("/api/test"))
        assertThat(list.whitelist).containsExactly("/api/test")
    }
}
