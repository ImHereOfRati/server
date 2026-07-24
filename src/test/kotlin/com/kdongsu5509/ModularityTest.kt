package com.kdongsu5509

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

    @Test
    @DisplayName("Agreement는 Terms에 의존하지만 Terms는 Agreement에 의존하지 않는다")
    fun getDirectDependencies_success_agreement_depends_on_terms_without_reverse_dependency() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val agreementModule = modules.getModuleByName("agreement").orElseThrow()
        val termsModule = modules.getModuleByName("terms").orElseThrow()

        // when
        val agreementDependencies = agreementModule.getDirectDependencies(modules)
        val termsDependencies = termsModule.getDirectDependencies(modules)

        // then
        assertThat(agreementDependencies.containsModuleNamed("terms")).isTrue()
        assertThat(termsDependencies.containsModuleNamed("agreement")).isFalse()
    }

    @Test
    @DisplayName("Agreement 이벤트와 Support 예외 패키지를 Named Interface로 공개한다")
    fun getNamedInterfaces_success_exposes_agreement_events_and_support_exceptions() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val agreementModule = modules.getModuleByName("agreement").orElseThrow()
        val supportModule = modules.getModuleByName("support").orElseThrow()

        // when
        val agreementEvents = agreementModule.namedInterfaces.getByName("events")
        val supportExceptions = supportModule.namedInterfaces.getByName("exceptions")

        // then
        assertThat(agreementEvents).isPresent()
        assertThat(supportExceptions).isPresent()
    }
}
