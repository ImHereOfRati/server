package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.friends.controller.dto.UpdateAliasRequest
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.service.FriendshipService
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

@WebMvcTest(FriendshipController::class)
class FriendshipControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var friendshipService: FriendshipService

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

    private val owner = User(
        id = UUID.randomUUID(),
        email = "owner@example.com",
        nickname = "owner-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val friend = User(
        id = UUID.randomUUID(),
        email = "friend@example.com",
        nickname = "friend-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val userDetails = ImHereUserDetails(
        email = owner.email,
        nickname = owner.nickname,
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
        const val BASE_PATH = "/api/friendships"
    }

    @Test
    @DisplayName("친구 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun readAll_success() {
        val friendship = Friendship(
            id = UUID.randomUUID(),
            owner = owner,
            friend = friend,
            friendAlias = "베프",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(friendship), PageRequest.of(0, 10), false)

        given(friendshipService.findAllByOwnerEmail(eq(owner.email), any()))
            .willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(friendship.id.toString()))
    }

    @Test
    @DisplayName("전체 친구 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun readAll_admin_success() {
        val friendship = Friendship(
            id = UUID.randomUUID(),
            owner = owner,
            friend = friend,
            friendAlias = "베프",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(friendship), PageRequest.of(0, 10), false)

        given(friendshipService.findAll(any()))
            .willReturn(slice)

        mockMvc.perform(
            get("$BASE_PATH/admin")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(friendship.id.toString()))
    }

    @Test
    @DisplayName("특정 유저와 친구 여부 확인 시 200 OK와 여부를 Boolean으로 반환한다")
    fun checkFriendStatus_success() {
        val targetUserId = friend.id!!
        val friendship = Friendship(
            id = UUID.randomUUID(),
            owner = owner,
            friend = friend,
            friendAlias = "베프",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendshipService.findByOwnerEmailAndFriendId(eq(owner.email), eq(targetUserId)))
            .willReturn(friendship)

        mockMvc.perform(
            get("$BASE_PATH/target/$targetUserId")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
    }

    @Test
    @DisplayName("친구 관계 단건 조회 시 200 OK와 상세 정보를 반환한다")
    fun readById_success() {
        val friendshipId = UUID.randomUUID()
        val friendship = Friendship(
            id = friendshipId,
            owner = owner,
            friend = friend,
            friendAlias = "베프",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendshipService.findByIdAndOwnerEmail(eq(friendshipId), eq(owner.email)))
            .willReturn(friendship)

        mockMvc.perform(
            get("$BASE_PATH/$friendshipId")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.data.friendAlias").value("베프"))
    }

    @Test
    @DisplayName("친구 삭제 시 204 No Content를 반환한다")
    fun deleteFriendship_success() {
        val friendshipId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$friendshipId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("친구 삭제 시 204 No Content를 반환한다")
    fun deleteFriendship_admin_success() {
        val friendshipId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/admin/$friendshipId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("친구 별칭 수정 성공 시 200 OK와 수정 정보를 반환한다")
    fun updateAlias_success() {
        val friendshipId = UUID.randomUUID()
        val requestDto = UpdateAliasRequest(alias = "새별칭")
        val updatedFriendship = Friendship(
            id = friendshipId,
            owner = owner,
            friend = friend,
            friendAlias = "새별칭",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendshipService.updateAliasByIdAndOwnerEmail(eq(friendshipId), eq(owner.email), eq("새별칭")))
            .willReturn(updatedFriendship)

        mockMvc.perform(
            patch("$BASE_PATH/$friendshipId/alias")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(friendshipId.toString()))
            .andExpect(jsonPath("$.data.friendAlias").value("새별칭"))
    }

    @Test
    @DisplayName("친구 차단 성공 시 200 OK를 반환한다")
    fun blockFriend_success() {
        val friendshipId = UUID.randomUUID()

        mockMvc.perform(
            post("$BASE_PATH/$friendshipId/block")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }
}
