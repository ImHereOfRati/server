package com.kdongsu5509.friends.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.service.FriendRelationAdminCommandService
import com.kdongsu5509.friends.service.FriendRelationAdminQueryService
import com.kdongsu5509.friends.service.dto.*
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import java.time.LocalDateTime
import java.util.*

@ImHereLightWebMvcTest(controllers = [FriendRelationAdminController::class])
class FriendRelationAdminControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var friendRelationAdminQueryService: FriendRelationAdminQueryService

    @MockitoBean
    private lateinit var friendRelationAdminCommandService: FriendRelationAdminCommandService

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    private val admin = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    private val low = FriendMember(UUID.randomUUID(), "low@example.com", "low")
    private val high = FriendMember(UUID.randomUUID(), "high@example.com", "high")

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)
    private val pageable = PageRequest.of(0, 20)

    @Test
    @DisplayName("요청 목록 경로가 유지된다")
    fun requests_path_kept() {
        // given
        given(friendRelationAdminQueryService.findAllRequests(any()))
            .willReturn(
                SliceImpl(
                    listOf(FriendRequestView(UUID.randomUUID(), low, high, "친구가 되고 싶습니다", now, now)),
                    pageable,
                    false
                )
            )

        // when & then
        mockMvc.perform(get("/api/admin/friend-requests").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].requester.email").value(low.email))
    }

    @Test
    @DisplayName("친구 목록 경로가 유지된다")
    fun friendships_path_kept() {
        // given
        given(friendRelationAdminQueryService.findAllFriendships(any()))
            .willReturn(
                SliceImpl(
                    listOf(FriendshipView(UUID.randomUUID(), low, high, low.nickname, now, now)),
                    pageable,
                    false
                )
            )

        // when & then
        mockMvc.perform(get("/api/admin/friendships").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].owner.email").value(low.email))
    }

    @Test
    @DisplayName("제한 목록 경로가 유지된다")
    fun restrictions_path_kept() {
        // given
        given(friendRelationAdminQueryService.findAllRestrictions(any()))
            .willReturn(
                SliceImpl(
                    listOf(
                        FriendRestrictionView(
                            UUID.randomUUID(), low, high, FriendRestrictionType.BLOCK, now, now, null
                        )
                    ),
                    pageable,
                    false
                )
            )

        // when & then
        mockMvc.perform(get("/api/admin/friend-restrictions").with(user(admin)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].type").value("BLOCK"))
    }
}
