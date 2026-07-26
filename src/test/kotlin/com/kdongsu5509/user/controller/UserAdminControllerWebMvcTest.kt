package com.kdongsu5509.user.controller

import com.kdongsu5509.user.service.UserLifecycleService
import com.kdongsu5509.user.service.UserQueryService
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.api.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import java.util.*

@WebMvcTest(UserAdminController::class)
class UserAdminControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userQueryService: UserQueryService

    @MockitoBean
    private lateinit var userLifecycleService: UserLifecycleService

    @MockitoBean
    private lateinit var accessLogPrinter: AccessLogPrinter

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @MockitoBean
    private lateinit var tokenParser: ImHereTokenParserPort

    @MockitoBean
    private lateinit var securityWhiteList: SecurityWhiteList

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val BASE_PATH = "/api/admin/users"
    }

    @TestConfiguration
    @EnableMethodSecurity
    class MethodSecurityConfig

    @Test
    @DisplayName("관리자 권한으로 전체 사용자 조회 시 성공하고 200 OK와 상세 정보 슬라이스를 반환한다")
    fun readAll_success_when_admin() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
        val userId = UUID.randomUUID()
        val userResult = UserResult(
            id = userId,
            email = "user@example.com",
            nickname = "일반유저",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        val pageable = PageRequest.of(0, 15)
        val slice = SliceImpl(listOf(userResult), pageable, false)

        given(userQueryService.findAll(any())).willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(adminDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(userId.toString()))
            .andExpect(jsonPath("$.data.content[0].role").value("NORMAL"))
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.hasNext").value(false))
    }

    @Test
    @DisplayName("관리자가 사용자를 차단하면 대상 이메일을 생명주기 서비스에 전달한다")
    fun block_delegates_target_email() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")

        mockMvc.perform(
            post("$BASE_PATH/{email}/block", "target@example.com")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isOk)

        verify(userLifecycleService).block("target@example.com")
    }

    @Test
    @DisplayName("관리자가 사용자 차단을 해제하면 대상 이메일을 생명주기 서비스에 전달한다")
    fun unblock_delegates_target_email() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")

        mockMvc.perform(
            delete("$BASE_PATH/{email}/block", "target@example.com")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isOk)

        verify(userLifecycleService).unblock("target@example.com")
    }

    @Test
    @DisplayName("관리자가 강제 로그아웃을 요청하면 대상 이메일을 생명주기 서비스에 전달한다")
    fun forceLogout_delegates_target_email() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")

        mockMvc.perform(
            delete("$BASE_PATH/{email}/token", "target@example.com")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isOk)

        verify(userLifecycleService).requestForceLogout("target@example.com")
    }

    @Test
    @DisplayName("관리자가 사용자를 탈퇴시키면 대상 이메일을 생명주기 서비스에 전달한다")
    fun withdraw_delegates_target_email() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")

        mockMvc.perform(
            delete("$BASE_PATH/{email}", "target@example.com")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isOk)

        verify(userLifecycleService).withdraw("target@example.com")
    }
}
