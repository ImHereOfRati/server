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
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
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
        // given
        userRepository.save(UserJpaEntity(
            email = "me@example.com",
            nickname = "MeNick",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ))

        // when & then
        mockMvc.perform(
            get("/api/users/my")
                .with(user(myDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("me@example.com"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-read-me-success",
                    snippets = arrayOf(
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
    }

    @Test
    @DisplayName("키워드로 타 사용자를 페이징 조회한다")
    fun readOthersSuccess() {
        // given
        userRepository.save(UserJpaEntity(
            email = "me@example.com",
            nickname = "MeNick",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ))
        userRepository.save(UserJpaEntity(
            email = "friend1@example.com",
            nickname = "홍길동",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ))
        userRepository.save(UserJpaEntity(
            email = "friend2@example.com",
            nickname = "홍길순",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ))

        // when & then
        mockMvc.perform(
            get("/api/users")
                .param("keyword", "홍길동")
                .param("page", "0")
                .param("size", "10")
                .with(user(myDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "users-read-others-success",
                    snippets = arrayOf(
                        queryParameters(
                            parameterWithName("keyword").description("검색어 (이메일 또는 닉네임)"),
                            parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                            parameterWithName("size").description("페이지 크기").optional()
                        ),
                        relaxedResponseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.content[].id").description("사용자 ID"),
                            fieldWithPath("data.content[].email").description("이메일"),
                            fieldWithPath("data.content[].nickname").description("닉네임"),
                            fieldWithPath("data.content[].oAuth2Provider").description("로그인 제공자"),
                            fieldWithPath("data.hasNext").description("다음 페이지 존재 여부")
                        )
                    )
                )
            )
    }

}
