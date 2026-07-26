package com.kdongsu5509

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

class ModularityTest {

    @Test
    @Disabled(
        """
        잔여 위반 해소 후 활성화한다.
        - 사이클 7건: friends <-> user, auth <-> user, auth <-> agreement 및 파생 경로
        - non-exposed 116건
          [정리 대상] user -> friends.repository.jpa Q타입(15)
          [보류] friends -> user.repository.jpa/UserMapper(90),
                 auth -> agreement.service(6), notifications -> auth.application.port.out(5)
        보류분은 agreement/user/terms가 의존하는 방향이 아니라 그 반대 방향이므로 후순위로 둔다.
        """
    )
    @DisplayName("모든 모듈이 서로의 Named Interface만 통해 의존한다")
    fun verify_success_no_module_boundary_violation() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)

        // when & then
        modules.verify()
    }

    @Test
    @DisplayName("Shared 모듈은 하위 패키지를 공개하는 Open 모듈이다")
    fun isOpen_success_shared_module_is_open() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)

        // when
        val sharedModule = modules.getModuleByName("shared").orElseThrow()

        // then
        assertThat(sharedModule.isOpen).isTrue()
    }

    @Test
    @DisplayName("Agreement는 User API에 의존하지만 User는 Agreement에 의존하지 않는다")
    fun getDirectDependencies_success_agreement_depends_on_user_without_reverse_dependency() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val userModule = modules.getModuleByName("user").orElseThrow()
        val agreementModule = modules.getModuleByName("agreement").orElseThrow()

        // when
        val userDependencies = userModule.getDirectDependencies(modules)
        val agreementDependencies = agreementModule.getDirectDependencies(modules)

        // then
        assertThat(agreementDependencies.containsModuleNamed("user")).isTrue()
        assertThat(userDependencies.containsModuleNamed("agreement")).isFalse()
    }

    @Test
    @DisplayName("공유 보안 애노테이션을 Auth 모듈의 Shared Named Interface로 공개한다")
    fun getNamedInterfaces_success_exposes_security_shared() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val authModule = modules.getModuleByName("auth").orElseThrow()

        // when
        val sharedInterface = authModule.namedInterfaces.getByName("shared")

        // then
        assertThat(sharedInterface).isPresent()
    }

    @Test
    @DisplayName("User 도메인 타입을 Named Interface로 공개한다")
    fun getNamedInterfaces_success_exposes_user_domain_types() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val userModule = modules.getModuleByName("user").orElseThrow()

        // when
        val userDomain = userModule.namedInterfaces.getByName("domain").orElseThrow()
        val exposedTypes = userDomain.asJavaClasses().map { it.name }.toList()

        // then
        assertThat(exposedTypes).contains(
            "com.kdongsu5509.user.domain.UserStatus",
            "com.kdongsu5509.user.domain.UserRole",
            "com.kdongsu5509.user.domain.OAuth2Provider",
        )
    }

    @Test
    @DisplayName("User 모듈은 외부 조회와 등록과 세션 제어 계약만 API Named Interface로 공개한다")
    fun getNamedInterfaces_success_exposes_user_lookup_api() {
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val userModule = modules.getModuleByName("user").orElseThrow()

        val userApi = userModule.namedInterfaces.getByName("api").orElseThrow()
        val exposedTypes = userApi.asJavaClasses().map { it.name }.toList()

        assertThat(exposedTypes).contains(
            "com.kdongsu5509.user.api.RegisterUserCommand",
            "com.kdongsu5509.user.api.UserLookupContract",
            "com.kdongsu5509.user.api.UserRegistrationContract",
            "com.kdongsu5509.user.api.UserActivationContract",
            "com.kdongsu5509.user.api.UserResult",
        )
        assertThat(exposedTypes).doesNotContain(
            "com.kdongsu5509.user.repository.UserRepository",
            "com.kdongsu5509.user.service.UserQueryService",
            "com.kdongsu5509.user.service.UserProfileService",
            "com.kdongsu5509.user.service.UserLifecycleService",
            "com.kdongsu5509.user.service.UserRegistrationService",
        )
    }

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
    @DisplayName("Support 모듈은 하위 패키지를 공개하는 Open 모듈이다")
    fun isOpen_success_support_module_is_open() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)

        // when
        val supportModule = modules.getModuleByName("support").orElseThrow()

        // then
        assertThat(supportModule.isOpen).isTrue()
    }

    @Test
    @DisplayName("Agreement는 이벤트가 아닌 User API 계약으로 사용자 활성화를 요청한다")
    fun getNamedInterfaces_success_agreement_exposes_no_event_interface() {
        // given
        val modules = ApplicationModules.of(ImhereApplication::class.java)
        val agreementModule = modules.getModuleByName("agreement").orElseThrow()
        val userModule = modules.getModuleByName("user").orElseThrow()

        // when
        val agreementEvents = agreementModule.namedInterfaces.getByName("events")
        val userApi = userModule.namedInterfaces.getByName("api").orElseThrow()

        // then
        assertThat(agreementEvents).isEmpty()
        assertThat(userApi.asJavaClasses().map { it.name }.toList())
            .contains("com.kdongsu5509.user.api.UserActivationContract")
    }
}
