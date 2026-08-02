package com.kdongsu5509.user.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.support.external.UserErrorAlertNotifier
import com.kdongsu5509.user.controller.dto.NicknameUpdateRequest
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserLifecycleService
import com.kdongsu5509.user.service.UserProfileService
import com.kdongsu5509.user.api.UserResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
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

@ImHereLightWebMvcTest(controllers = [UserCommandController::class])
class UserCommandControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userProfileService: UserProfileService

    @MockitoBean
    private lateinit var userLifecycleService: UserLifecycleService

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

    @Autowired
    private lateinit var objectMapper: JsonMapper

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
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
        val request = NicknameUpdateRequest(nickname = "새닉네임")
        val userId = UUID.randomUUID()
        val result = UserResult(
            id = userId,
            email = "sender@example.com",
            nickname = "새닉네임",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )

        given(userProfileService.updateNickname(eq("sender@example.com"), eq("새닉네임"))).willReturn(result)

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
    }

    @Test
    @DisplayName("닉네임이 비어 있으면 400 Bad Request를 반환한다")
    fun updateMe_fail_when_nickname_blank() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val request = NicknameUpdateRequest(nickname = "")

        // when & then
        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
    }

    @Test
    @DisplayName("변경하려는 닉네임이 7자를 초과하면 400 Bad Request를 반환한다")
    fun updateMe_fail_when_nickname_too_long() {
        // given
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")
        val request = NicknameUpdateRequest(nickname = "여덟글자짜리닉네임")

        // when & then
        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
    }

    @Test
    @DisplayName("인증이 안 된 상태로 내 정보 수정을 요청하면 401 Unauthorized를 반환한다")
    fun updateMe_fail_unauthorized() {
        val request = NicknameUpdateRequest(nickname = "새닉네임")

        mockMvc.perform(
            patch(UPDATE_ME_PATH)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("인증된 사용자가 탈퇴하면 204 응답을 반환하고 이메일을 생명주기 서비스에 전달한다")
    fun withdraw_returns_no_content_and_delegates_authenticated_email() {
        val userDetails = ImHereUserDetails("sender@example.com", "sender-nick", "ROLE_USER", "ACTIVE")

        mockMvc.perform(
            delete("$UPDATE_ME_PATH/withdrawal")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)

        verify(userLifecycleService).withdraw("sender@example.com")
    }
}
