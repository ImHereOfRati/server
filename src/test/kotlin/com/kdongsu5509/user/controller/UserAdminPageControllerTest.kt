package com.kdongsu5509.user.controller

import com.kdongsu5509.support.external.UserErrorAlertNotifier
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.then
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.util.UUID

@WebMvcTest(controllers = [UserAdminPageController::class])
@AutoConfigureMockMvc(addFilters = false)
class UserAdminPageControllerTest {

    companion object {
        const val USER_ADMIN_PAGE_URL = "/admin/users"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userQueryService: UserQueryService

    @MockitoBean
    private lateinit var userErrorAlertNotifier: UserErrorAlertNotifier

    @Test
    @DisplayName("사용자 관리 페이지는 조회 결과와 다음 페이지 존재 여부를 모델에 담아 admin/users 뷰를 반환한다")
    fun page_success() {
        val user = userResult()
        given(userQueryService.findAll(any())).willReturn(SliceImpl(listOf(user), PageRequest.of(0, 20), true))

        mockMvc.perform(get(USER_ADMIN_PAGE_URL).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(view().name("admin/users"))
            .andExpect(model().attribute("users", listOf(user)))
            .andExpect(model().attribute("hasNext", true))
    }

    @Test
    @DisplayName("페이지 파라미터를 생략하면 0페이지 20건으로 조회한다")
    fun page_success_with_default_pageable() {
        given(userQueryService.findAll(any())).willReturn(SliceImpl(emptyList(), PageRequest.of(0, 20), false))

        mockMvc.perform(get(USER_ADMIN_PAGE_URL).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(model().attribute("hasNext", false))

        val captor = argumentCaptor<Pageable>()
        then(userQueryService).should().findAll(captor.capture())
        assertThat(captor.firstValue.pageNumber).isEqualTo(0)
        assertThat(captor.firstValue.pageSize).isEqualTo(20)
    }

    @Test
    @DisplayName("페이지 파라미터를 주면 그대로 조회에 전달한다")
    fun page_success_with_explicit_pageable() {
        given(userQueryService.findAll(any())).willReturn(SliceImpl(emptyList(), PageRequest.of(2, 5), false))

        mockMvc.perform(get(USER_ADMIN_PAGE_URL).with(csrf()).param("page", "2").param("size", "5"))
            .andExpect(status().isOk)

        val captor = argumentCaptor<Pageable>()
        then(userQueryService).should().findAll(captor.capture())
        assertThat(captor.firstValue.pageNumber).isEqualTo(2)
        assertThat(captor.firstValue.pageSize).isEqualTo(5)
    }

    private fun userResult() = UserResult(
        id = UUID.randomUUID(),
        email = "user@example.com",
        nickname = "일반유저",
        oauthProvider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = UserStatus.ACTIVE,
    )
}
