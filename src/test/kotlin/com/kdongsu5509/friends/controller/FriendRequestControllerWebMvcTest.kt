package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.friends.controller.dto.FriendRequestViewType
import com.kdongsu5509.friends.controller.dto.NewFriendRequest
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.service.FriendRequestService
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.domain.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(FriendRequestController::class)
class FriendRequestControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var friendRequestService: FriendRequestService

    @MockitoBean
    private lateinit var accessLogPrinter: AccessLogPrinter

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @MockitoBean
    private lateinit var tokenParser: ImHereTokenParserPort

    @MockitoBean
    private lateinit var securityWhiteList: SecurityWhiteList

    @Autowired
    private lateinit var objectMapper: JsonMapper

    private val requester = User(
        id = UUID.randomUUID(),
        email = "requester@example.com",
        nickname = "requester-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val receiver = User(
        id = UUID.randomUUID(),
        email = "receiver@example.com",
        nickname = "receiver-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val userDetails = ImHereUserDetails(
        email = requester.email,
        nickname = requester.nickname,
        role = "ROLE_USER",
        status = "ACTIVE"
    )

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val BASE_PATH = "/api/friends/requests"
    }

    @Test
    @DisplayName("친구 요청 성공 시 200 OK와 생성된 친구 요청 ID를 반환한다")
    fun request_success() {
        val requestDto = NewFriendRequest(
            targetId = receiver.id!!,
            message = "안녕하세요. 친하게 지내요!"
        )
        val friendRequest = FriendRequest(
            id = UUID.randomUUID(),
            requester = requester,
            receiver = receiver,
            message = requestDto.message,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendRequestService.request(eq(requester.email), eq(requestDto.targetId), eq(requestDto.message)))
            .willReturn(friendRequest)

        mockMvc.perform(
            post(BASE_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.friendRequestId").value(friendRequest.id.toString()))
    }

    @Test
    @DisplayName("전체 친구 요청 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun findAll_admin_success() {
        val friendRequest = FriendRequest(
            id = UUID.randomUUID(),
            requester = requester,
            receiver = receiver,
            message = "안녕하세요. 친하게 지내요!",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(friendRequest), PageRequest.of(0, 10), false)

        given(friendRequestService.findAll(any())).willReturn(slice)

        mockMvc.perform(
            get("$BASE_PATH/admin")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(friendRequest.id.toString()))
    }

    @Test
    @DisplayName("보내거나 받은 친구 요청 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun findSentOrReceivedAll_success() {
        val friendRequest = FriendRequest(
            id = UUID.randomUUID(),
            requester = requester,
            receiver = receiver,
            message = "안녕하세요. 친하게 지내요!",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(friendRequest), PageRequest.of(0, 10), false)

        given(friendRequestService.findAllByEmailAndType(eq(requester.email), eq(FriendRequestViewType.SENT), any()))
            .willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .param("type", "SENT")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(friendRequest.id.toString()))
    }

    @Test
    @DisplayName("친구 요청 단건 조회 시 200 OK와 정보를 반환한다")
    fun readById_success() {
        val requestId = UUID.randomUUID()
        val friendRequest = FriendRequest(
            id = requestId,
            requester = requester,
            receiver = receiver,
            message = "안녕하세요. 친하게 지내요!",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendRequestService.findByIdAndParticipantEmail(eq(requestId), eq(requester.email)))
            .willReturn(friendRequest)

        mockMvc.perform(
            get("$BASE_PATH/$requestId")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(requestId.toString()))
            .andExpect(jsonPath("$.data.message").value("안녕하세요. 친하게 지내요!"))
    }

    @Test
    @DisplayName("친구 요청 수락 시 200 OK와 친구 관계 생성 정보를 반환한다")
    fun acceptFriendRequest_success() {
        val requestId = UUID.randomUUID()
        val friendship = Friendship(
            id = UUID.randomUUID(),
            owner = receiver,
            friend = requester,
            friendAlias = "",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val receiverUserDetails = ImHereUserDetails(
            email = receiver.email,
            nickname = receiver.nickname,
            role = "ROLE_USER",
            status = "ACTIVE"
        )

        given(friendRequestService.acceptRequest(eq(receiver.email), eq(requestId)))
            .willReturn(friendship)

        mockMvc.perform(
            post("$BASE_PATH/$requestId/accept")
                .with(csrf())
                .with(user(receiverUserDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(friendship.id.toString()))
    }

    @Test
    @DisplayName("친구 요청 거절 시 200 OK와 차단 정보를 반환한다")
    fun rejectFriendRequest_success() {
        val requestId = UUID.randomUUID()
        val restriction = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = receiver,
            restricted = requester,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val receiverUserDetails = ImHereUserDetails(
            email = receiver.email,
            nickname = receiver.nickname,
            role = "ROLE_USER",
            status = "ACTIVE"
        )

        given(friendRequestService.rejectRequest(eq(receiver.email), eq(requestId)))
            .willReturn(restriction)

        mockMvc.perform(
            post("$BASE_PATH/$requestId/reject")
                .with(csrf())
                .with(user(receiverUserDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(restriction.id.toString()))
    }

    @Test
    @DisplayName("수신받은 친구 요청 삭제(거절/삭제) 시 200 OK를 반환한다")
    fun delete_success() {
        val requestId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$requestId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("보낸 친구 요청 취소 시 200 OK를 반환한다")
    fun cancelSentRequest_success() {
        val requestId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$requestId/sent")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("특정 친구 요청 삭제 시 200 OK를 반환한다")
    fun deleteById_admin_success() {
        val requestId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/admin/$requestId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }
}
