package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(UserUpdatePersistenceAdapter::class)
class UserUpdatePersistenceAdapterTest @Autowired constructor(
    private val userUpdatePersistenceAdapter: UserUpdatePersistenceAdapter,
    private val springDataUserRepository: SpringDataUserRepository
) {

    @Test
    @DisplayName("PENDING 상태인 사용자를 ACTIVE로 변경한다")
    fun activate_success() {
        // given
        val email = "pending@kakao.com"
        val pendingUser = UserJpaEntity(
            email = email,
            nickname = "테스터",
            provider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.PENDING
        )
        springDataUserRepository.save(pendingUser)

        // when
        userUpdatePersistenceAdapter.activate(email)

        // then
        val updatedUser = springDataUserRepository.findByEmail(email)
        assertThat(updatedUser).isNotNull
        assertThat(updatedUser?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("이미 ACTIVE 상태인 사용자는 상태 변경이 일어나지 않는다")
    fun activate_already_active() {
        // given
        val email = "active@kakao.com"
        val activeUser = UserJpaEntity(
            email = email,
            nickname = "활성유저",
            provider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        springDataUserRepository.save(activeUser)

        // when
        userUpdatePersistenceAdapter.activate(email)

        // then
        val result = springDataUserRepository.findByEmail(email)
        assertThat(result?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("사용자가 없으면 오류가 발생한다")
    fun user_not_exist() {
        // given
        val notUserEmail = "none@kakao.com"

        // when, then
        assertThatThrownBy {
            userUpdatePersistenceAdapter.activate(notUserEmail)
        }.isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_NOT_FOUND)
    }
}