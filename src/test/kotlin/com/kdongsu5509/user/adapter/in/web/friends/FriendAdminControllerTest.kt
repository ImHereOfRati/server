package com.kdongsu5509.user.adapter.`in`.web.friends

import com.common.testUtil.ControllerTestSupport
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FriendAdminControllerTest : ControllerTestSupport() {

    companion object {
        const val BASE_URL = "/api/admin/friends"
    }

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var relationshipsRepository: SpringDataFriendRelationshipsRepository

    @Autowired
    lateinit var requestRepository: SpringDataFriendRequestRepository

    // ─── #73 Force Clear Friend Relationship ─────────────────────────────────

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자가 두 유저 간 친구 관계를 강제 삭제한다")
    fun forceClearFriendRelationship_success() {
        // given
        val userA = userRepository.save(activeUser("adminA@kakao.com"))
        val userB = userRepository.save(activeUser("adminB@kakao.com"))
        relationshipsRepository.save(FriendRelationshipsJpaEntity(userA, userB, "B"))
        relationshipsRepository.save(FriendRelationshipsJpaEntity(userB, userA, "A"))

        // when & then
        mockMvc.perform(
            delete(BASE_URL)
                .param("userA", userA.email)
                .param("userB", userB.email)
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(relationshipsRepository.findAll()).isEmpty()
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자가 두 유저 간 친구 요청을 강제 삭제한다")
    fun forceClearFriendRequest_success() {
        // given
        val requester = userRepository.save(activeUser("req@kakao.com"))
        val receiver = userRepository.save(activeUser("rcv@kakao.com"))
        requestRepository.save(FriendRequestJpaEntity(requester, receiver, "친구해요"))

        // when & then
        mockMvc.perform(
            delete("$BASE_URL/requests")
                .param("requester", requester.email)
                .param("receiver", receiver.email)
                .with(csrf())
        ).andExpect(status().isOk)

        assertThat(requestRepository.findAll()).isEmpty()
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("관리자 권한 없이 친구 관계 강제 삭제 시 403을 반환한다")
    fun forceClearFriendRelationship_forbidden() {
        mockMvc.perform(
            delete(BASE_URL)
                .param("userA", "a@kakao.com")
                .param("userB", "b@kakao.com")
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
}
