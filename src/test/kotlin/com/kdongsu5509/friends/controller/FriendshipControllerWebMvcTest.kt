package com.kdongsu5509.friends.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.service.FriendRelationCommandService
import com.kdongsu5509.friends.service.FriendRelationQueryService
import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.friends.service.dto.FriendshipView
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.*

@ImHereLightWebMvcTest(controllers = [FriendshipController::class])
class FriendshipControllerWebMvcTest {

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
    private val friendId = UUID.fromString("00000000-0000-0000-0000-000000000002")

    private val principal = ImHereUserDetails(
        email = "me@example.com",
        nickname = "me",
        role = "NORMAL",
        status = "ACTIVE",
        userId = requesterId
    )

    private val me = FriendMember(requesterId, "me@example.com", "me")
    private val friend = FriendMember(friendId, "friend@example.com", "friend")

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

    @Test
    @DisplayName("친구 목록 응답은 owner/friend/friendAlias 형태를 유지한다")
    fun readAll_keeps_response_shape() {
        // given
        given(friendRelationQueryService.findFriends(any(), any()))
            .willReturn(SliceImpl(listOf(friendshipView()), PageRequest.of(0, 20), false))

        // when & then
        mockMvc.perform(get("/api/friendships").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].owner.email").value(me.email))
            .andExpect(jsonPath("$.data.content[0].friend.email").value(friend.email))
            .andExpect(jsonPath("$.data.content[0].friendAlias").value(me.nickname))
    }

    @Test
    @DisplayName("친구 여부 조회는 관계 유무를 boolean으로 준다")
    fun checkFriendStatus_returns_boolean() {
        // given
        given(friendRelationQueryService.findFriendByTarget(eq(requesterId), eq(friendId))).willReturn(null)

        // when & then
        mockMvc.perform(get("/api/friendships/target/$friendId").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(false))
    }

    @Test
    @DisplayName("단건 조회 응답도 같은 형태를 유지한다")
    fun readById_keeps_response_shape() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationQueryService.findFriend(eq(id), eq(requesterId))).willReturn(friendshipView(id))

        // when & then
        mockMvc.perform(get("/api/friendships/$id").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(id.toString()))
            .andExpect(jsonPath("$.data.owner.email").value(me.email))
    }

    @Test
    @DisplayName("별칭 변경은 변경된 별칭을 응답에 담는다")
    fun updateAlias_returns_updated_alias() {
        // given
        val id = UUID.randomUUID()
        given(friendRelationCommandService.updateAlias(eq(id), eq(requesterId), eq("단짝")))
            .willReturn(FriendshipView(id, me, friend, "단짝", now, now))

        // when & then
        mockMvc.perform(
            patch("/api/friendships/$id/alias")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(mapOf("alias" to "단짝")))
                .with(user(principal))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendAlias").value("단짝"))
    }

    @Test
    @DisplayName("친구 삭제는 204를 준다")
    fun delete_returns_no_content() {
        // given: 삭제는 반환값이 없어 준비할 것이 없다.

        // when & then
        mockMvc.perform(delete("/api/friendships/${UUID.randomUUID()}").with(user(principal)).with(csrf()))
            .andExpect(status().isNoContent)
    }

    /** 수락 시점에 내 자리 별칭이 내 닉네임으로 채워진 상태를 흉내 낸다. */
    private fun friendshipView(id: UUID = UUID.randomUUID()) =
        FriendshipView(id, me, friend, me.nickname, now, now)
}
