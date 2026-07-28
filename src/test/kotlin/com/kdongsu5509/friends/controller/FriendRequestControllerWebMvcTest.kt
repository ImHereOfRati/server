package com.kdongsu5509.friends.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.service.FriendRelationCommandService
import com.kdongsu5509.friends.service.FriendRelationQueryService
import com.kdongsu5509.friends.service.dto.*
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.*

@ImHereLightWebMvcTest(controllers = [FriendRequestController::class])
class FriendRequestControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var friendRelationCommandService: FriendRelationCommandService

    @MockitoBean
    private lateinit var friendRelationQueryService: FriendRelationQueryService

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    private val requesterId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val principal = ImHereUserDetails(
        email = "me@example.com",
        nickname = "me",
        role = "NORMAL",
        status = "ACTIVE",
        userId = requesterId
    )

    private val me = FriendMember(requesterId, "me@example.com", "me")
    private val other = FriendMember(otherId, "other@example.com", "other")

    private val message = "친구가 되고 싶습니다"
    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

    @Test
    @DisplayName("요청 생성은 생성된 요청 식별자를 준다")
    fun request_returns_id() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationCommandService.sendRequest(eq(requesterId), eq(otherId), eq(message)))
            .willReturn(requestView(id))

        // when & then
        mockMvc.perform(
            post("/api/friends/requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    jsonMapper.writeValueAsString(
                        mapOf("targetId" to otherId.toString(), "message" to message)
                    )
                )
                .with(user(principal))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendRequestId").value(id.toString()))
    }

    @Test
    @DisplayName("요청 목록 응답은 requester/receiver/message 형태를 유지한다")
    fun findAll_keeps_response_shape() {
        // given
        given(friendRelationQueryService.findRequests(any(), any(), any()))
            .willReturn(SliceImpl(listOf(requestView()), PageRequest.of(0, 20), false))

        // when & then
        mockMvc.perform(get("/api/friends/requests?type=SENT").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].requester.email").value(me.email))
            .andExpect(jsonPath("$.data.content[0].receiver.email").value(other.email))
            .andExpect(jsonPath("$.data.content[0].message").value(message))
    }

    @Test
    @DisplayName("수락 응답은 친구 관계 형태다")
    fun accept_returns_friendship_shape() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationCommandService.acceptRequest(eq(id), eq(requesterId)))
            .willReturn(FriendshipView(id, me, other, me.nickname, now, now))

        // when & then
        mockMvc.perform(post("/api/friends/requests/$id/accept").with(user(principal)).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.owner.email").value(me.email))
            .andExpect(jsonPath("$.data.friend.email").value(other.email))
            .andExpect(jsonPath("$.data.friendAlias").value(me.nickname))
    }

    @Test
    @DisplayName("거절 응답은 제한 형태이고 type이 REJECT다")
    fun reject_returns_restriction_shape() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationCommandService.rejectRequest(eq(id), eq(requesterId)))
            .willReturn(
                FriendRestrictionView(id, me, other, FriendRestrictionType.REJECT, now, now, now.plusMonths(1))
            )

        // when & then
        mockMvc.perform(post("/api/friends/requests/$id/reject").with(user(principal)).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.restrictor.email").value(me.email))
            .andExpect(jsonPath("$.data.restricted.email").value(other.email))
            .andExpect(jsonPath("$.data.type").value("REJECT"))
    }

    private fun requestView(id: UUID = UUID.randomUUID()) =
        FriendRequestView(id, me, other, message, now, now)
}
