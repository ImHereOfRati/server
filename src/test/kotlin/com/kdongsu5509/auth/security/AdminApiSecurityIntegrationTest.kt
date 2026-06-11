package com.kdongsu5509.auth.security

import com.common.testsupport.WebIntegrationTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminApiSecurityIntegrationTest : WebIntegrationTestSupport() {

    private val adminDetails = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    private val normalUserDetails = ImHereUserDetails(
        email = "user@example.com",
        nickname = "user",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("인증되지 않은 사용자가 관리자 API에 접근하면 401 Unauthorized를 반환한다")
    fun unauthenticatedRequestToAdminApiReturnsUnauthorized() {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("일반 사용자가 관리자 API에 접근하면 403 Forbidden을 반환한다")
    fun nonAdminRequestToAdminApiReturnsForbidden() {
        mockMvc.perform(
            get("/api/admin/users")
                .with(user(normalUserDetails))
        ).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("기존 사용자 관리자 조회 경로는 더 이상 사용되지 않는다")
    fun legacyUserAdminPathRemoved() {
        mockMvc.perform(
            get("/api/users")
                .with(user(adminDetails))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기존 약관 관리자 조회 경로는 더 이상 사용되지 않는다")
    fun legacyTermsAdminPathRemoved() {
        mockMvc.perform(
            get("/api/terms")
                .with(user(adminDetails))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기존 친구 요청 관리자 조회 경로는 더 이상 사용되지 않는다")
    fun legacyFriendRequestAdminPathRemoved() {
        mockMvc.perform(
            get("/api/friends/requests/admin")
                .with(user(adminDetails))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("기존 친구 차단 관리자 조회 경로는 더 이상 사용되지 않는다")
    fun legacyFriendRestrictionAdminPathRemoved() {
        mockMvc.perform(
            get("/api/friends/restrictions/admin")
                .with(user(adminDetails))
        ).andExpect(status().isMethodNotAllowed)
    }

    @Test
    @DisplayName("기존 친구 관계 관리자 조회 경로는 더 이상 사용되지 않는다")
    fun legacyFriendshipAdminPathRemoved() {
        mockMvc.perform(
            get("/api/friendships/admin")
                .with(user(adminDetails))
        ).andExpect(status().isBadRequest)
    }
}
