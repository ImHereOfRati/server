package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.auth.security.ImHereUserDetails
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserLoginIntegrationTest : WebIntegrationTestSupport() {

    private val userDetails = ImHereUserDetails(
        email = "user@example.com",
        nickname = "testUser",
        role = "NORMAL",
        status = "ACTIVE"
    )

    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("인증되지 않은 사용자는 API 접근이 제한된다")
    fun unauthenticatedUserCannotAccessApi() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("일반 사용자는 관리자 API에 접근할 수 없다")
    fun normalUserCannotAccessAdminApi() {
        mockMvc.perform(
            get("/api/admin/users")
                .with(user(userDetails))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("ADMIN 역할을 가진 사용자는 관리자 API에 접근할 수 있다")
    fun adminUserCanAccessAdminApi() {
        mockMvc.perform(
            get("/api/admin/users")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("관리자는 관리자 페이지에 접근할 수 있다")
    fun adminCanAccessAdminPages() {
        mockMvc.perform(
            get("/admin")
                .with(user(adminDetails))
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("일반 사용자는 관리자 페이지에 접근할 수 없다")
    fun normalUserCannotAccessAdminPages() {
        mockMvc.perform(
            get("/admin")
                .with(user(userDetails))
        )
            .andExpect(status().isForbidden)
    }
}
