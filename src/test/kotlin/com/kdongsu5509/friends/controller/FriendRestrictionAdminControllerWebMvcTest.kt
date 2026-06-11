package com.kdongsu5509.friends.controller

import com.kdongsu5509.auth.application.port.out.ImHereTokenParserPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.auth.security.SecurityWhiteList
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
import java.util.UUID

@WebMvcTest(FriendRestrictionAdminController::class)
class FriendRestrictionAdminControllerWebMvcTest {

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

    private val restrictor = User(UUID.randomUUID(), "restrictor@example.com", "restrictor-nick", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
    private val restricted = User(UUID.randomUUID(), "restricted@example.com", "restricted-nick", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)

    @BeforeEach
    fun setUp(webApplicationContext: WebApplicationContext) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }

    companion object {
        const val BASE_PATH = "/api/admin/friend-restrictions"
    }

    @Test
    @DisplayName("관리자가 전체 차단 목록 조회 시 200 OK와 페이징된 목록을 반환한다")
    fun findAll_success() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
        val restriction = FriendRestriction(UUID.randomUUID(), restrictor, restricted, FriendRestrictionType.BLOCK, LocalDateTime.now(), LocalDateTime.now())
        val slice = SliceImpl(listOf(restriction), PageRequest.of(0, 10), false)

        given(friendRestrictionService.findAll(any())).willReturn(slice)

        mockMvc.perform(
            get(BASE_PATH)
                .with(user(adminDetails))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.imhereResponseCode").value("SUCCESS"))
            .andExpect(jsonPath("$.data.content[0].id").value(restriction.id.toString()))
    }

    @Test
    @DisplayName("관리자가 차단 내역 삭제 시 200 OK를 반환한다")
    fun deleteById_success() {
        val adminDetails = ImHereUserDetails("admin@example.com", "admin", "ADMIN", "ACTIVE")
        val restrictionId = UUID.randomUUID()

        mockMvc.perform(
            delete("$BASE_PATH/$restrictionId")
                .with(csrf())
                .with(user(adminDetails))
        ).andExpect(status().isOk)
    }
}
