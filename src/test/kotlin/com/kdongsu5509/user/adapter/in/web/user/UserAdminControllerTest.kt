package com.kdongsu5509.user.adapter.`in`.web.user

import com.common.testUtil.ControllerTestSupport
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.redis.RedisCacheAdapter
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

class UserAdminControllerTest : ControllerTestSupport() {

    companion object {
        const val BASE_URL = "/api/admin/users"
    }

    @Autowired
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Autowired
    lateinit var redisCacheAdapter: RedisCacheAdapter

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자가 특정 유저의 리프레시 토큰을 강제 삭제(로그아웃)한다")
    fun forceLogout_success() {
        // given
        val email = "victim@kakao.com"
        val redisKey = "refresh:$email"
        redisCacheAdapter.save(redisKey, "some-refresh-token", Duration.ofDays(7))

        // when & then
        mockMvc.perform(
            delete("$BASE_URL/{email}/token", email)
                .with(csrf())
        ).andExpect(status().isOk)

        val remaining = redisCacheAdapter.find(redisKey, String::class.java)
        assertThat(remaining).isNull()
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("관리자 권한 없이 강제 로그아웃 요청 시 403을 반환한다")
    fun forceLogout_forbidden() {
        mockMvc.perform(
            delete("$BASE_URL/{email}/token", "victim@kakao.com")
                .with(csrf())
        ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자가 특정 유저를 차단한다")
    fun blockUser_success() {
        // given
        val email = "badguy@kakao.com"
        springDataUserRepository.save(activeUser(email))

        // when & then
        mockMvc.perform(
            post("$BASE_URL/{email}/block", email)
                .with(csrf())
        ).andExpect(status().isOk)

        val blocked = springDataUserRepository.findByEmail(email)
        assertThat(blocked?.status).isEqualTo(UserStatus.BLOCKED)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자가 차단된 유저를 차단 해제한다")
    fun unblockUser_success() {
        // given
        val email = "blocked@kakao.com"
        springDataUserRepository.save(blockedUser(email))

        // when & then
        mockMvc.perform(
            delete("$BASE_URL/{email}/block", email)
                .with(csrf())
        ).andExpect(status().isOk)

        val unblocked = springDataUserRepository.findByEmail(email)
        assertThat(unblocked?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("관리자 권한 없이 차단 요청 시 403을 반환한다")
    fun blockUser_forbidden() {
        mockMvc.perform(
            post("$BASE_URL/{email}/block", "badguy@kakao.com")
                .with(csrf())
        ).andExpect(status().isForbidden)
    }

    private fun activeUser(email: String) = UserJpaEntity(
        email = email,
        nickname = "테스터",
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun blockedUser(email: String) = UserJpaEntity(
        email = email,
        nickname = "차단유저",
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = UserStatus.BLOCKED
    )
}
