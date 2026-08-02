package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.friends.service.dto.FriendRequestView
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.friends.service.dto.FriendshipView
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserQueryService
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
import java.time.LocalDateTime
import java.util.UUID.randomUUID

class AdminWebIntegrationTest : WebIntegrationTestSupport() {

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

    @MockitoBean
    private lateinit var failedNotificationAdminService: NotificationUseCase

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
    @DisplayName("관리자는 실패 알림 관리 페이지에 접근할 수 있다")
    fun failedNotificationPageAccessibleForAdmin() {
        whenever(failedNotificationAdminService.findAll(NotificationStatus.DEAD, 0, 100)).thenReturn(
            listOf(
                Notification.reconstruct(
                    id = 1L,
                    dedupeKey = "event:FCM",
                    targetIdentifier = "receiver@example.com",
                    method = NotificationMethod.FCM,
                    senderAlias = "sender",
                    type = NotificationType.FRIEND_REQUEST_RECEIVED,
                    title = "제목",
                    body = "본문",
                    extraData = emptyMap(),
                    status = NotificationStatus.DEAD,
                    attempts = 3,
                    lastError = "FCM 실패",
                    sentAt = null,
                    isRead = false,
                    createdAt = now,
                )
            )
        )

        mockMvc.perform(
            get("/admin/failed-notifications")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("실패 알림 관리")))
            .andExpect(content().string(containsString("receiver@example.com")))
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
            listOf(TermResult(1L, 1L, TermTypes.SERVICE, "서비스 이용약관", "내용", LocalDateTime.now(), true))
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
                        randomUUID(), restrictor, restricted, FriendRelationStatus.BLOCKED, now, now, null
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
            .andExpect(content().string(containsString("BLOCKED")))
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
