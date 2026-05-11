package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.support.exception.type.NotFoundException
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(UserUpdatePersistenceAdapter::class, UserMapper::class)
class UserUpdatePersistenceAdapterTest @Autowired constructor(
    private val userUpdatePersistenceAdapter: UserUpdatePersistenceAdapter,
    private val springDataUserRepository: SpringDataUserRepository
) {

    companion object {
        const val PENDING_USER_EMAIL = "pending@kakao.com"
        const val ACTIVE_USER_EMAIL = "active@kakao.com"
        const val BLOCKED_USER_EMAIL = "blocked@kakao.com"

    }

    @BeforeEach
    fun setUp() {
        val pendingUser = createUserJpaEntity(PENDING_USER_EMAIL, UserStatus.PENDING)
        val activeUser = createUserJpaEntity(ACTIVE_USER_EMAIL, UserStatus.ACTIVE)
        val blockedUser = createUserJpaEntity(BLOCKED_USER_EMAIL, UserStatus.BLOCKED)

        springDataUserRepository.saveAll(listOf(pendingUser, activeUser, blockedUser))
    }

    @Test
    @DisplayName("PENDING 상태의 사용자를 ACTIVE로 변경한다")
    fun activate_success() {
        // when
        userUpdatePersistenceAdapter.activate(PENDING_USER_EMAIL)

        // then
        val updatedUser = springDataUserRepository.findByEmail(PENDING_USER_EMAIL)
        assertThat(updatedUser).isNotNull
        assertThat(updatedUser?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("이미 ACTIVE 상태인 사용자는 상태 변경이 일어나지 않는다")
    fun activate_already_active() {
        // when
        userUpdatePersistenceAdapter.activate(ACTIVE_USER_EMAIL)

        // then
        val result = springDataUserRepository.findByEmail(ACTIVE_USER_EMAIL)
        assertThat(result?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("활성화 시도 시에 사용자가 없으면 예외가 발생한다")
    fun activate_fail_when_user_not_found() {
        // given
        val notExistUserEmail = "none@kakao.com"

        // when & then
        assertThatThrownBy { userUpdatePersistenceAdapter.activate(notExistUserEmail) }
            .isInstanceOf(NotFoundException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("사용자의 닉네임을 성공적으로 변경한다")
    fun updateNickname_success() {
        //given
        val newNickname = "새로운_닉네임"

        // when
        val result = userUpdatePersistenceAdapter.updateNickname(ACTIVE_USER_EMAIL, newNickname)

        // then
        assertThat(result.nickname).isEqualTo(newNickname)
    }

    @Test
    @DisplayName("닉네임 변경 시에 사용자가 없으면 예외가 발생한다")
    fun updateNickname_fail_when_user_not_found() {
        // given
        val notUserEmail = "none@kakao.com"
        val notNewUserNickname = "notExist"

        // when & then
        assertThatThrownBy {
            userUpdatePersistenceAdapter.updateNickname(notUserEmail, notNewUserNickname)
        }.isInstanceOf(NotFoundException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("ACTIVE 사용자를 BLOCKED로 차단한다")
    fun block_success() {
        // when
        userUpdatePersistenceAdapter.block(ACTIVE_USER_EMAIL)

        // then
        val result = springDataUserRepository.findByEmail(ACTIVE_USER_EMAIL)
        assertThat(result?.status).isEqualTo(UserStatus.BLOCKED)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 차단 시 예외가 발생한다")
    fun block_fail_when_user_not_found() {
        // when & then
        assertThatThrownBy { userUpdatePersistenceAdapter.block("nobody@kakao.com") }
            .isInstanceOf(NotFoundException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("BLOCKED 사용자를 ACTIVE로 차단 해제한다")
    fun unblock_success() {
        // when
        userUpdatePersistenceAdapter.unblock(BLOCKED_USER_EMAIL)

        // then
        val result = springDataUserRepository.findByEmail(BLOCKED_USER_EMAIL)
        assertThat(result?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 차단 해제 시 예외가 발생한다")
    fun unblock_fail_when_user_not_found() {
        // when & then
        assertThatThrownBy { userUpdatePersistenceAdapter.unblock("nobody@kakao.com") }
            .isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    private fun createUserJpaEntity(email: String, status: UserStatus): UserJpaEntity = UserJpaEntity(
        email = email,
        nickname = "테스트",
        provider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = status
    )
}
