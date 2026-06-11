package com.kdongsu5509.user.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserAdminControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "AdminNick",
        role = "ADMIN",
        status = "ACTIVE"
    )

    private val myDetails = ImHereUserDetails(
        email = "me@example.com",
        nickname = "MeNick",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("관리자는 모든 사용자 정보를 조회할 수 있다")
    fun readAllAdminSuccess() {
        userRepository.save(UserJpaEntity(
            email = "user1@example.com",
            nickname = "User1",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ))

        mockMvc.perform(
            get("/api/admin/users")
                .with(user(adminDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].email").value("user1@example.com"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-read-all-admin-success",
                    snippets = arrayOf(
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.content[].id").description("사용자 ID"),
                            fieldWithPath("data.content[].email").description("이메일"),
                            fieldWithPath("data.content[].nickname").description("닉네임"),
                            fieldWithPath("data.content[].role").description("사용자 역할"),
                            fieldWithPath("data.content[].oAuth2Provider").description("로그인 제공자"),
                            fieldWithPath("data.content[].status").description("계정 상태"),
                            fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("일반 사용자가 전체 조회를 시도하면 403 Forbidden 발생")
    fun readAllUserForbidden() {
        mockMvc.perform(
            get("/api/admin/users")
                .with(user(myDetails))
        ).andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-read-all-forbidden"
                )
            )
    }
}
