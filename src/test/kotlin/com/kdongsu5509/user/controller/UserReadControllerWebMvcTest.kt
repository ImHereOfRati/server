package com.kdongsu5509.user.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.support.external.UserErrorAlertNotifier
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserQueryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
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
import java.util.*

@ImHereLightWebMvcTest(controllers = [UserReadController::class])
class UserReadControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userQueryService: UserQueryService

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val BASE_PATH = "/api/users"
    }

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    class MethodSecurityConfig

    @Test
    @DisplayName("로그인한 상태로 내 정보 조회 요청 시 200 OK와 사용자 정보를 반환한다")
    fun readMe_success() {
        // given
        val userId = UUID.randomUUID()
        val userDetails =
            ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE", userId)
        val result = UserResult(
            id = userId,
            email = "sender@example.com",
            nickname = "sender-nick",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )

        given(userQueryService.findById(eq(userId))).willReturn(result)

        // when & then
        mockMvc.perform(
            get("$BASE_PATH/my")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value("sender@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("sender-nick"))
            .andExpect(jsonPath("$.data.oAuth2Provider").value("KAKAO"))
    }

    @Test
    @DisplayName("인증 정보 없이 내 정보 조회를 요청하면 401 Unauthorized를 반환한다")
    fun readMe_fail_unauthorized() {
        mockMvc.perform(
            get("$BASE_PATH/my")
        ).andExpect(status().isUnauthorized)
    }

}
