package com.kdongsu5509.friends.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.repository.jpa.FriendRelationJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRelationRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FriendCandidateControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var friendRelationRepository: SpringDataFriendRelationRepository

    @Test
    @DisplayName("키워드로 친구 후보를 페이징 조회한다")
    fun findCandidatesSuccess() {
        // given
        val me = saveUser("me@example.com", "MeNick")
        saveUser("friend1@example.com", "홍길동")
        saveUser("friend2@example.com", "홍길순")

        // when & then
        mockMvc.perform(
            get("/api/users")
                .param("keyword", "홍길동")
                .param("page", "0")
                .param("size", "10")
                .with(user(principalOf(me)))
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
                        responseFields(
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

    @Test
    @DisplayName("이미 관계가 있는 사용자는 후보에서 빠진다")
    fun findCandidatesExcludesRelatedUsers() {
        // given
        val me = saveUser("me@example.com", "MeNick")
        val related = saveUser("friend1@example.com", "홍길동")
        saveRelation(me, related)

        // when & then
        mockMvc.perform(
            get("/api/users")
                .param("keyword", "홍길동")
                .with(user(principalOf(me)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(0))
    }

    private fun saveUser(email: String, nickname: String): UserJpaEntity =
        userRepository.save(
            UserJpaEntity(
                email = email,
                nickname = nickname,
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )

    private fun saveRelation(one: UserJpaEntity, other: UserJpaEntity) {
        val (low, high) = listOf(one.id!!, other.id!!).sorted()
        friendRelationRepository.save(
            FriendRelationJpaEntity(
                lowUserId = low,
                highUserId = high,
                status = FriendRelationStatus.ACCEPTED,
                initiatedUserId = one.id!!
            )
        )
    }

    private fun principalOf(entity: UserJpaEntity) = ImHereUserDetails(
        email = entity.email,
        nickname = entity.nickname,
        role = "USER",
        status = "ACTIVE",
        userId = entity.id
    )
}
