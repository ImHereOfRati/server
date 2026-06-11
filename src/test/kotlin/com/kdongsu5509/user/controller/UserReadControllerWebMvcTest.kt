package com.kdongsu5509.user.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
import java.util.*

@WebMvcTest(UserReadController::class)
class UserReadControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

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
        const val BASE_PATH = "/api/users"
    }

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    class MethodSecurityConfig

    @Test
    @DisplayName("로그인한 상태로 내 정보 조회 요청 시 200 OK와 사용자 정보를 반환한다")
    fun readMe_success() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val userId = UUID.randomUUID()
        val result = UserResult(
            id = userId,
            email = "sender@example.com",
            nickname = "sender-nick",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )

        given(userService.findByEmail(eq("sender@example.com"))).willReturn(result)

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

    @Test
    @DisplayName("키워드 파라미터가 비어있으면 400 Bad Request를 반환한다")
    fun readOthers_fail_when_keyword_blank() {
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            get(BASE_PATH)
                .param("keyword", "")
                .with(user(userDetails))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("키워드로 타인 조회 시 성공하고 200 OK와 유저 슬라이스를 반환한다")
    fun readOthers_success() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val otherId = UUID.randomUUID()
        val otherUser = UserResult(
            id = otherId,
            email = "other@example.com",
            nickname = "검색대상",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        val pageable = PageRequest.of(0, 15)
        val slice = SliceImpl(listOf(otherUser), pageable, false)

        given(userService.findByKeyword(eq("sender@example.com"), eq("검색대상"), any())).willReturn(slice)

        // when & then
        mockMvc.perform(
            get(BASE_PATH)
                .param("keyword", "검색대상")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(otherId.toString()))
            .andExpect(jsonPath("$.data.content[0].email").value("other@example.com"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("검색대상"))
            .andExpect(jsonPath("$.data.hasNext").value(false))
    }

}
