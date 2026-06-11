package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.DlqQueueInfoResponse
import com.kdongsu5509.notifications.application.service.DlqAdminService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminWebIntegrationTest : WebIntegrationTestSupport() {

    @MockitoBean
    private lateinit var dlqAdminService: DlqAdminService

    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("관리자 로그인 페이지는 인증 없이 접근할 수 있다")
    fun loginPageAccessibleWithoutAuthentication() {
        mockMvc.perform(get("/admin/login"))
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 로그인")))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 대시보드에 접근하면 로그인 페이지로 이동한다")
    fun dashboardRedirectsWhenUnauthenticated() {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/login"))
    }

    @Test
    @DisplayName("관리자는 관리자 대시보드에 접근할 수 있다")
    fun dashboardAccessibleForAdmin() {
        mockMvc.perform(
            get("/admin")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("관리자 대시보드")))
    }

    @Test
    @DisplayName("관리자는 DLQ 관리 페이지에 접근할 수 있다")
    fun dlqPageAccessibleForAdmin() {
        whenever(dlqAdminService.getAllDlqInfo()).thenReturn(
            listOf(DlqQueueInfoResponse(queueName = "friend.dlq", messageCount = 3, consumerCount = 1))
        )

        mockMvc.perform(
            get("/admin/dead-letter-queues")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("DLQ 관리")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("friend.dlq")))
    }

    @Test
    @DisplayName("관리자 API는 Security CORS 설정에 따라 preflight 요청을 허용한다")
    fun adminApiCorsPreflightAllowed() {
        mockMvc.perform(
            options("/api/admin/users")
                .header("Origin", "https://fortuneki.site")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://fortuneki.site"))
    }
}
