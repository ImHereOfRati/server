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

    @Test
    @DisplayName("관리자 actuator base-path를 permitAll 경로에 합칠 수 있다")
    fun permitAllPathsIncludesActuatorPaths() {
        val list = SecurityWhiteList(whitelist = listOf("/api/test"))

        assertThat(list.permitAllPaths("/custom-actuator"))
            .containsExactly(
                "/api/test",
                "/custom-actuator",
                "/custom-actuator/**"
            )
    }

    @Test
    @DisplayName("base-path가 루트(/)면 루트 전체를 permitAll로 연다")
    fun permitAllPathsOpensEverythingWhenBasePathIsRoot() {
        val list = SecurityWhiteList(whitelist = listOf("/api/test"))

        assertThat(list.permitAllPaths("/"))
            .containsExactly("/api/test", "/", "/**")
    }

    @Test
    @DisplayName("base-path가 비어 있으면 기본값 /actuator를 사용한다")
    fun permitAllPathsFallsBackToDefaultActuatorPath() {
        val list = SecurityWhiteList()

        assertThat(list.permitAllPaths("   "))
            .containsExactly("/actuator", "/actuator/**")
    }

    @Test
    @DisplayName("base-path에 앞 슬래시가 없으면 붙이고, 뒤 슬래시는 떼어 낸다")
    fun permitAllPathsNormalizesSlashes() {
        val list = SecurityWhiteList()

        assertThat(list.permitAllPaths("manage/"))
            .containsExactly("/manage", "/manage/**")
    }

    @Test
    @DisplayName("whitelist와 actuator 경로가 겹치면 중복을 제거한다")
    fun permitAllPathsRemovesDuplicates() {
        val list = SecurityWhiteList(whitelist = listOf("/actuator", "/api/test"))

        assertThat(list.permitAllPaths("/actuator"))
            .containsExactly("/actuator", "/api/test", "/actuator/**")
    }
}
