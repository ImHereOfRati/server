package com.kdongsu5509

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class AuthModularityTest {

    private val modules = ApplicationModules.of(ImhereApplication::class.java)
    private val authModule = modules.getModuleByName("auth").orElseThrow()

    @Test
    @DisplayName("auth 모듈은 다른 모듈의 Named Interface만 통해 의존한다")
    fun verifyDependencies_success_auth_module_has_no_violation() {
        authModule.verifyDependencies(modules)
    }

    @Test
    @DisplayName("auth 모듈이 의존하는 모듈을 기록한다")
    fun getDirectDependencies_success_auth_depends_only_on_user() {
        // when
        val dependencies = authModule.getDirectDependencies(modules)

        // then
        assertThat(dependencies.containsModuleNamed("user")).isTrue()
        assertThat(dependencies.containsModuleNamed("friends")).isFalse()
        assertThat(dependencies.containsModuleNamed("notifications")).isFalse()
    }
}
