package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.service.FriendshipService
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import java.time.LocalDateTime
import java.util.*

@WebMvcTest(FriendshipAdminController::class)
class FriendshipAdminControllerWebMvcTest {

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

    private val owner = User(
        UUID.randomUUID(),
        "owner@example.com",
        "owner-nick",
        UserRole.NORMAL,
        OAuth2Provider.KAKAO,
        UserStatus.ACTIVE
    )
    private val friend = User(
        UUID.randomUUID(),
        "friend@example.com",
        "friend-nick",
        UserRole.NORMAL,
        OAuth2Provider.KAKAO,
        UserStatus.ACTIVE
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
        const val BASE_PATH = "/api/admin/friendships"
    }

    @Test
    @DisplayName("관리자가 전체 친구 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun readAll_success() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
        val friendship = Friendship(UUID.randomUUID(), owner, friend, "베프", LocalDateTime.now(), LocalDateTime.now())
        val slice = SliceImpl(listOf(friendship), PageRequest.of(0, 10), false)

        given(friendshipService.findAll(any())).willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(adminDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(friendship.id.toString()))
    }

    @Test
    @DisplayName("관리자가 친구 삭제 시 204 No Content를 반환한다")
    fun delete_success() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
        val friendshipId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$friendshipId")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isNoContent)
    }
}
