package com.kdongsu5509.user.controller

import com.kdongsu5509.auth.application.port.`in`.ForceLogoutUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import java.util.UUID

@WebMvcTest(UserAdminController::class)
class UserAdminControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var forceLogoutUseCase: ForceLogoutUseCase

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

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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

        given(userService.findAll(any())).willReturn(slice)

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

}
