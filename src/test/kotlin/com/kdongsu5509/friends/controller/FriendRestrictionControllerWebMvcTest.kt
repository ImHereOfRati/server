package com.kdongsu5509.friends.controller

import com.common.testsupport.ImHereLightWebMvcTest
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.friends.service.FriendRelationCommandService
import com.kdongsu5509.friends.service.FriendRelationQueryService
import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.friends.service.dto.FriendRestrictionView
import com.kdongsu5509.support.external.UserErrorAlertNotifier
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

@ImHereLightWebMvcTest(controllers = [FriendRestrictionController::class])
class FriendRestrictionControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @MockitoBean
    private lateinit var friendRelationCommandService: FriendRelationCommandService

    @MockitoBean
    private lateinit var friendRelationQueryService: FriendRelationQueryService

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

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

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 27, 12, 0)

    @Test
    @DisplayName("제한 목록 응답은 restrictor/restricted/type 형태를 유지한다")
    fun findAll_keeps_response_shape() {
        // given
        given(friendRelationQueryService.findRestrictions(any(), any()))
            .willReturn(SliceImpl(listOf(blockView()), PageRequest.of(0, 20), false))

        // when & then
        mockMvc.perform(get("/api/friends/restrictions").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content[0].restrictor.email").value(me.email))
            .andExpect(jsonPath("$.data.content[0].restricted.email").value(other.email))
            .andExpect(jsonPath("$.data.content[0].type").value("BLOCKED"))
    }

    @Test
    @DisplayName("차단 응답의 type은 BLOCKED이다")
    fun block_returns_block_type() {
        // given
        given(friendRelationCommandService.block(eq(requesterId), eq(otherId))).willReturn(blockView())

        // when & then
        mockMvc.perform(
            post("/api/friends/restrictions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(mapOf("targetUserId" to otherId.toString())))
                .with(user(principal))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.type").value("BLOCKED"))
            .andExpect(jsonPath("$.data.restrictor.email").value(me.email))
    }

    @Test
    @DisplayName("제한 여부 조회는 boolean을 준다")
    fun checkRestrictionStatus_returns_boolean() {
        // given
        given(friendRelationQueryService.existsRestriction(eq(requesterId), eq(otherId))).willReturn(true)

        // when & then
        mockMvc.perform(get("/api/friends/restrictions/target/$otherId").with(user(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
    }

    private fun blockView(id: UUID = UUID.randomUUID()) =
        FriendRestrictionView(id, me, other, FriendRelationStatus.BLOCKED, now, now, null)
}
