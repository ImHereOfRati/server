package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.ImHereUserDetails
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class AdminOttIntegrationTest : WebIntegrationTestSupport() {

    private val adminDetails = ImHereUserDetails(
        email = "rati0806",
        nickname = "rati",
        role = "ADMIN",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("관리자가 OTT 요청 페이지에 접근할 수 있다")
    fun ottPageAccessibleForAdmin() {
        mockMvc.perform(get("/admin/ott"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("관리자 OTT 인증")))
    }

    @Test
    @DisplayName("관리자가 OTT 요청 페이지에 username 파라미터와 함께 접근할 수 있다")
    fun ottPageWithUsernameParameter() {
        mockMvc.perform(get("/admin/ott").param("username", "rati0806"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("rati0806")))
    }

    @Test
    @DisplayName("관리자가 OTT 요청 페이지에서 error 파라미터를 받으면 에러 메시지를 표시한다")
    fun ottPageShowsErrorMessage() {
        mockMvc.perform(get("/admin/ott").param("error", "true"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("유효하지 않은 OTT입니다")))
    }

    @Test
    @DisplayName("OTT 로그인 페이지 기본 접근 확인")
    fun ottLoginPageBasicAccess() {
        mockMvc.perform(get("/admin/ott"))
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("OTT")))
    }

    @Test
    @DisplayName("로그인 후 관리자 대시보드에 접근할 수 있다")
    fun dashboardAccessibleAfterOttLogin() {
        mockMvc.perform(
            get("/admin")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("관리자 대시보드")))
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 대시보드에 접근하면 로그인 페이지로 리다이렉트된다")
    fun dashboardRedirectsToLoginForUnauthenticated() {
        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/login"))
    }

    @Test
    @DisplayName("로그아웃 후 관리자 대시보드에 접근할 수 없다")
    fun dashboardUnaccessibleAfterLogout() {
        mockMvc.perform(
            post("/admin/logout")
                .with(user(adminDetails))
                .with(csrf())
        )
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/login?logout=true"))

        mockMvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection)
            .andExpect(redirectedUrl("/admin/login"))
    }
}
