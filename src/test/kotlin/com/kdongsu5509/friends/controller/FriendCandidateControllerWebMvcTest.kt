package com.kdongsu5509.friends.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.service.FriendCandidateSearchService
import com.kdongsu5509.support.external.UserErrorAlertNotifier
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@ImHereLightWebMvcTest(controllers = [FriendCandidateController::class])
class FriendCandidateControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var friendCandidateSearchService: FriendCandidateSearchService

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

    private val requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    private val principal = ImHereUserDetails(
        email = "me@example.com",
        nickname = "me",
        role = "NORMAL",
        status = "ACTIVE",
        userId = requesterId
    )

    @Test
    @DisplayName("키워드 파라미터가 비어있으면 400 Bad Request를 반환한다")
    fun findCandidates_fail_when_keyword_blank() {
        mockMvc.perform(
            get("/api/users")
                .param("keyword", "")
                .with(user(principal))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("키워드로 친구 후보를 조회하면 200 OK와 사용자 슬라이스를 반환한다")
    fun findCandidates_success() {
        // given
        val otherId = UUID.randomUUID()
        val other = UserResult(
            id = otherId,
            email = "other@example.com",
            nickname = "검색대상",
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        val pageable = PageRequest.of(0, 15)
        given(
            friendCandidateSearchService.search(
                eq(requesterId),
                eq("검색대상"),
                any()
            )
        )
            .willReturn(
                SliceImpl(listOf(other), pageable, false)
            )

        // when & then
        mockMvc.perform(
            get("/api/users")
                .param("keyword", "검색대상")
                .with(user(principal))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(otherId.toString()))
            .andExpect(jsonPath("$.data.content[0].email").value("other@example.com"))
            .andExpect(jsonPath("$.data.content[0].nickname").value("검색대상"))
            .andExpect(jsonPath("$.data.content[0].oAuth2Provider").value("KAKAO"))
            .andExpect(jsonPath("$.data.hasNext").value(false))
    }
}
