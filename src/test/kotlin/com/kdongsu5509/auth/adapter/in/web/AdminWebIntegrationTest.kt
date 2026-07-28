package com.kdongsu5509.auth.adapter.`in`.web

import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.friends.service.dto.FriendRestrictionType
import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import com.kdongsu5509.friends.service.dto.FriendRequestView
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.friends.service.dto.FriendshipView
import com.kdongsu5509.user.service.UserQueryService
import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.application.dto.DlqQueueInfo
import com.kdongsu5509.notifications.application.service.DlqAdminService
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.api.UserResult
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.UUID.randomUUID

class AdminWebIntegrationTest : WebIntegrationTestSupport() {

    private val now: java.time.LocalDateTime = java.time.LocalDateTime.of(2026, 7, 27, 12, 0)

    @MockitoBean
    private lateinit var dlqAdminService: DlqAdminService

    @MockitoBean
    private lateinit var userQueryService: UserQueryService

    @MockitoBean
    private lateinit var termService: TermService

    @MockitoBean
    private lateinit var friendRelationAdminQueryService: FriendRelationAdminQueryService

    @MockitoBean
    private lateinit var friendRelationAdminCommandService: FriendRelationAdminCommandService



    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("관리자 로그인 페이지는 인증 없이 접근할 수 있다")
    fun loginPageAccessibleWithoutAuthentication() {
        mockMvc.perform(get("/admin/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("관리자 로그인")))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 대시보드에 접근하면 로그인 페이지로 이동한다")
    fun dashboardRedirectsWhenUnauthenticated() {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/login"))
    }

    @Test
    @DisplayName("관리자는 관리자 대시보드에 접근할 수 있다")
    fun dashboardAccessibleForAdmin() {
        mockMvc.perform(
            get("/admin")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("관리자 대시보드")))
    }

    @Test
    @DisplayName("관리자는 DLQ 관리 페이지에 접근할 수 있다")
    fun dlqPageAccessibleForAdmin() {
        whenever(dlqAdminService.getAllDlqInfo()).thenReturn(
            listOf(DlqQueueInfo(queueName = "friend.dlq", messageCount = 3, consumerCount = 1))
        )

        mockMvc.perform(
            get("/admin/dead-letter-queues")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("DLQ 관리")))
            .andExpect(content().string(containsString("friend.dlq")))
    }

    @Test
    @DisplayName("관리자는 사용자 관리 페이지에 접근할 수 있다")
    fun usersPageAccessibleForAdmin() {
        whenever(userQueryService.findAll(any())).thenReturn(
            SliceImpl(
                listOf(
                    UserResult(
                        id = randomUUID(),
                        email = "user1@example.com",
                        nickname = "User1",
                        oauthProvider = OAuth2Provider.KAKAO,
                        role = UserRole.NORMAL,
                        status = UserStatus.ACTIVE
                    )
                ),
                PageRequest.of(0, 20),
                false
            )
        )

        mockMvc.perform(
            get("/admin/users")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("사용자 관리")))
            .andExpect(content().string(containsString("user1@example.com")))
    }

    @Test
    @DisplayName("관리자는 약관 관리 페이지에 접근할 수 있다")
    fun termsPageAccessibleForAdmin() {
        whenever(termService.findAll()).thenReturn(
            listOf(TermResult(1L, 1L, TermTypes.SERVICE, "서비스 이용약관", "내용", java.time.LocalDateTime.now(), true))
        )

        mockMvc.perform(
            get("/admin/terms")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("약관 관리")))
            .andExpect(content().string(containsString("서비스 이용약관")))
    }

    @Test
    @DisplayName("관리자는 친구 요청 관리 페이지에 접근할 수 있다")
    fun friendRequestsPageAccessibleForAdmin() {
        // given
        val requester = FriendMember(randomUUID(), "requester@example.com", "requester")
        val receiver = FriendMember(randomUUID(), "receiver@example.com", "receiver")
        whenever(friendRelationAdminQueryService.findAllRequests(any())).thenReturn(
            SliceImpl(
                listOf(FriendRequestView(randomUUID(), requester, receiver, "친구 요청 메시지입니다", now, now)),
                PageRequest.of(0, 20),
                false
            )
        )

        // when & then
        mockMvc.perform(
            get("/admin/friend-requests")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("친구 요청 관리")))
            .andExpect(content().string(containsString("친구 요청 메시지")))
    }

    @Test
    @DisplayName("관리자는 친구 차단 관리 페이지에 접근할 수 있다")
    fun friendRestrictionsPageAccessibleForAdmin() {
        // given
        val restrictor = FriendMember(randomUUID(), "restrictor@example.com", "restrictor")
        val restricted = FriendMember(randomUUID(), "restricted@example.com", "restricted")
        whenever(friendRelationAdminQueryService.findAllRestrictions(any())).thenReturn(
            SliceImpl(
                listOf(
                    FriendRestrictionView(
                        randomUUID(), restrictor, restricted, FriendRestrictionType.BLOCK, now, now, null
                    )
                ),
                PageRequest.of(0, 20),
                false
            )
        )

        // when & then
        mockMvc.perform(
            get("/admin/friend-restrictions")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("친구 차단 관리")))
            .andExpect(content().string(containsString("BLOCK")))
    }

    @Test
    @DisplayName("관리자는 친구 관계 관리 페이지에 접근할 수 있다")
    fun friendshipsPageAccessibleForAdmin() {
        // given
        val owner = FriendMember(randomUUID(), "owner@example.com", "owner")
        val friend = FriendMember(randomUUID(), "friend@example.com", "friend")
        whenever(friendRelationAdminQueryService.findAllFriendships(any())).thenReturn(
            SliceImpl(
                listOf(FriendshipView(randomUUID(), owner, friend, "베프", now, now)),
                PageRequest.of(0, 20),
                false
            )
        )

        // when & then
        mockMvc.perform(
            get("/admin/friendships")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("친구 관계 관리")))
            .andExpect(content().string(containsString("베프")))
    }

    @Test
    @DisplayName("관리자 API는 Security CORS 설정에 따라 preflight 요청을 허용한다")
    fun adminApiCorsPreflightAllowed() {
        mockMvc.perform(
            options("/api/admin/users")
                .header("Origin", "https://ratiko.co.kr")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://ratiko.co.kr"))
    }
}
