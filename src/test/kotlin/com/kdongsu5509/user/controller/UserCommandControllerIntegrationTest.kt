package com.kdongsu5509.user.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.user.controller.dto.UserUpdateRequest
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedRequestFields
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserCommandControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    private val userDetails = ImHereUserDetails(
        email = "sender@example.com",
        nickname = "senderNick",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("닉네임 변경 요청 시 성공적으로 DB에 반영되고 200 OK를 반환한다")
    fun updateMeSuccess() {
        // given
        val userEntity = UserJpaEntity(
            email = "sender@example.com",
            nickname = "senderNick",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        userRepository.save(userEntity)

        val request = UserUpdateRequest(nickname = "새닉네임")

        // when & then
        mockMvc.perform(
            patch("/api/users/my")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname").value("새닉네임"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-update-me-success",
                    snippets = arrayOf(
                        relaxedRequestFields(
                            fieldWithPath("nickname").description("변경할 닉네임 (최대 5자)").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.id").description("사용자 ID (UUID)"),
                            fieldWithPath("data.email").description("이메일"),
                            fieldWithPath("data.nickname").description("닉네임"),
                            fieldWithPath("data.oAuth2Provider").description("로그인 제공자")
                        )
                    )
                )
            )

        val updatedUser = userRepository.findByEmail("sender@example.com")!!
        assertThat(updatedUser.nickname).isEqualTo("새닉네임")
    }
}
