package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
import com.kdongsu5509.friends.controller.dto.CreateFriendRestrictionRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.service.FriendRestrictionService
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

@WebMvcTest(FriendRestrictionController::class)
class FriendRestrictionControllerWebMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var friendRestrictionService: FriendRestrictionService

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

    private val restrictor = User(
        id = UUID.randomUUID(),
        email = "restrictor@example.com",
        nickname = "restrictor-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val restricted = User(
        id = UUID.randomUUID(),
        email = "restricted@example.com",
        nickname = "restricted-nick",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private val userDetails = ImHereUserDetails(
        email = restrictor.email,
        nickname = restrictor.nickname,
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
        const val BASE_PATH = "/api/friends/restrictions"
    }

    @Test
    @DisplayName("차단 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun findAll_success() {
        val restriction = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = restrictor,
            restricted = restricted,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(restriction), PageRequest.of(0, 10), false)

        given(friendRestrictionService.findAllByRestrictorEmail(eq(restrictor.email), any()))
            .willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(restriction.id.toString()))
    }

    @Test
    @DisplayName("전체 차단 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun findAll_admin_success() {
        val restriction = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = restrictor,
            restricted = restricted,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val slice = SliceImpl(listOf(restriction), PageRequest.of(0, 10), false)

        given(friendRestrictionService.findAll(any()))
            .willReturn(slice)

        mockMvc.perform(
            get("$BASE_PATH/admin")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(restriction.id.toString()))
    }

    @Test
    @DisplayName("유저 차단 성공 시 200 OK와 차단 생성 정보를 반환한다")
    fun restrictUser_success() {
        val requestDto = CreateFriendRestrictionRequest(targetUserId = restricted.id!!)
        val restriction = FriendRestriction(
            id = UUID.randomUUID(),
            restrictor = restrictor,
            restricted = restricted,
            type = FriendRestrictionType.BLOCK,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        given(friendRestrictionService.restrictUser(eq(restrictor.email), eq(requestDto.targetUserId)))
            .willReturn(restriction)

        mockMvc.perform(
            post(BASE_PATH)
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(restriction.id.toString()))
            .andExpect(jsonPath("$.data.type").value("BLOCK"))
    }

    @Test
    @DisplayName("유저 차단 여부 확인 시 200 OK와 여부를 Boolean으로 반환한다")
    fun checkRestrictionStatus_success() {
        val targetUserId = restricted.id!!
        given(friendRestrictionService.existRestricted(eq(restrictor.email), eq(targetUserId)))
            .willReturn(true)

        mockMvc.perform(
            get("$BASE_PATH/target/$targetUserId")
                .with(user(userDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
    }

    @Test
    @DisplayName("차단 내역 삭제 시 200 OK를 반환한다")
    fun delete_success() {
        val restrictionId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$restrictionId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("차단 해제 (unblock) 시 200 OK를 반환한다")
    fun unblock_success() {
        val restrictedId = restricted.id!!

        mockMvc.perform(
            delete("$BASE_PATH/blocked-users/$restrictedId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("차단 내역 삭제 시 200 OK를 반환한다")
    fun deleteById_admin_success() {
        val restrictionId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/admin/$restrictionId")
                .with(csrf())
                .with(user(userDetails))
        ).andExpect(status().isOk)
    }
}
