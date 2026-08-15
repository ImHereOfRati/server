package com.kdongsu5509.user.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserReadControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    private val myDetails = ImHereUserDetails(
        email = "me@example.com",
        nickname = "MeNick",
        role = "USER",
        status = "ACTIVE"
    )

    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "AdminNick",
        role = "ADMIN",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("내 정보 조회를 성공한다")
    fun readMeSuccess() {
        // given: 내 정보 조회는 인증 주체의 식별자로 찾으므로 저장된 식별자를 principal에 실어 준다.
        val saved = userRepository.save(
            UserJpaEntity(
                email = "me@example.com",
                nickname = "MeNick",
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )

        // when & then
        mockMvc.perform(
            get("/api/users/my")
                .with(user(myDetails.copy(userId = saved.id)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("me@example.com"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-read-me-success",
                    snippets = arrayOf(
                        responseFields(
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
    }
}
