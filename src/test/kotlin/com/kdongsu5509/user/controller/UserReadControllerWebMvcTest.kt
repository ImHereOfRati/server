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
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
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
@ExtendWith(RestDocumentationExtension::class)
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
            .andDo(
                document(
                    "users/read-me-success",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("내 정보 조회")
                            .description("로그인한 본인의 이메일, 닉네임, 로그인 제공처 정보를 조회합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("인증 정보 없이 내 정보 조회를 요청하면 401 Unauthorized를 반환한다")
    fun readMe_fail_unauthorized() {
        mockMvc.perform(
            get("$BASE_PATH/my")
        ).andExpect(status().isUnauthorized)
            .andDo(
                document(
                    "users/read-me-unauthorized",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("내 정보 조회 실패 (인증 없음)")
                            .description("인증 정보 없이 내 정보 조회를 요청하면 401을 반환합니다.")
                            .build()
                    )
                )
            )
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
            .andDo(
                document(
                    "users/read-others-validation-fail",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("사용자 검색 실패 (키워드 공백)")
                            .description("검색어 파라미터(keyword)가 빈 문자열이면 400 오류를 반환합니다.")
                            .build()
                    )
                )
            )
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
            .andDo(
                document(
                    "users/read-others-success",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("사용자 검색")
                            .description("검색어(이메일 혹은 닉네임)로 다른 사용자를 검색하여 슬라이스 형식으로 반환합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("관리자 권한으로 전체 사용자 조회 시 성공하고 200 OK와 상세 정보 슬라이스를 반환한다")
    fun readAll_success_when_admin() {
        // given
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

        // when & then
        mockMvc.perform(
            get(BASE_PATH)
                .with(user(adminDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(userId.toString()))
            .andExpect(jsonPath("$.data.content[0].role").value("NORMAL"))
            .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.hasNext").value(false))
            .andDo(
                document(
                    "users/read-all-admin-success",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("[관리자] 전체 사용자 목록 조회")
                            .description("관리자 권한으로 모든 가입자의 상세 정보 목록을 슬라이스로 조회합니다.")
                            .build()
                    )
                )
            )
    }

    @Test
    @DisplayName("관리자 권한이 아닌 사용자가 전체 조회를 요청하면 403 Forbidden을 반환한다")
    fun readAll_fail_when_not_admin() {
        val userDetails = ImHereUserDetails("user@example.com", "user", "NORMAL", "ACTIVE")

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(userDetails))
        ).andExpect(status().isForbidden)
            .andDo(
                document(
                    "users/read-all-forbidden",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("사용자 - 조회")
                            .summary("[관리자] 전체 사용자 목록 조회 실패 (권한 없음)")
                            .description("일반 사용자 권한으로 전체 목록 조회를 요청하면 403 오류를 반환합니다.")
                            .build()
                    )
                )
            )
    }
}
