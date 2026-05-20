package com.kdongsu5509.user.controller

import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.controller.dto.UserUpdateRequest
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper
import java.util.*

@WebMvcTest(UserCommandController::class)
@ExtendWith(RestDocumentationExtension::class)
class UserCommandControllerWebMvcTest {

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

    @Autowired
    private lateinit var objectMapper: JsonMapper

    @BeforeEach
    fun setUp(
        webApplicationContext: WebApplicationContext,
        restDocumentation: RestDocumentationContextProvider
    ) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val UPDATE_ME_PATH = "/api/users/my"
    }

    @Test
    @DisplayName("닉네임 변경 요청 시 성공적으로 변경하고 200 OK를 반환한다")
    fun updateMe_success_with_nickname() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val request = UserUpdateRequest(nickname = "새닉네임")
        val userId = UUID.randomUUID()
        val result = UserResult(
            id = userId,
            email = "sender@example.com",
            nickname = "새닉네임",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )

        given(userService.updateNickname(eq("sender@example.com"), eq("새닉네임"))).willReturn(result)

        // when & then
        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value("sender@example.com"))
            .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
            .andExpect(jsonPath("$.data.oAuth2Provider").value("KAKAO"))
            .andDo(
                document(
                    "users/update-me-with-nickname",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 커맨드")
                            .summary("내 정보 수정 (닉네임)")
                            .description("로그인한 사용자의 닉네임을 변경하고 수정된 사용자 정보를 반환합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("닉네임 변경 요청에 닉네임이 없으면 기존 내 정보를 조회하여 200 OK를 반환한다")
    fun updateMe_success_with_null_nickname() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val request = UserUpdateRequest(nickname = null)
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
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(userId.toString()))
            .andExpect(jsonPath("$.data.nickname").value("sender-nick"))
            .andDo(
                document(
                    "users/update-me-no-nickname",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 커맨드")
                            .summary("내 정보 수정 (닉네임 변경 없음)")
                            .description("닉네임 변경 요청이 null인 경우 기존 정보를 반환합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("변경하려는 닉네임이 5자를 초과하면 400 Bad Request를 반환한다")
    fun updateMe_fail_when_nickname_too_long() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val request = UserUpdateRequest(nickname = "여섯글자닉네") // 6자

        // when & then
        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
            .andDo(
                document(
                    "users/update-me-validation-fail",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 커맨드")
                            .summary("내 정보 수정 실패 (유효성 검증 실패)")
                            .description("변경하려는 닉네임이 5자를 초과하는 경우 400 오류를 반환합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("인증이 안 된 상태로 내 정보 수정을 요청하면 401 Unauthorized를 반환한다")
    fun updateMe_fail_unauthorized() {
        val request = UserUpdateRequest(nickname = "새닉네임")

        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isUnauthorized)
            .andDo(
                document(
                    "users/update-me-unauthorized",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 커맨드")
                            .summary("내 정보 수정 실패 (인증 없음)")
                            .description("인증 정보 없이 내 정보 수정을 시도하면 401을 반환합니다.")
                            .build()
                    )
                )
            )
    }
}
