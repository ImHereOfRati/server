package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(UserUpdatePersistenceAdapter::class, UserMapper::class)
class UserUpdatePersistenceAdapterBlockTest @Autowired constructor(
    private val adapter: UserUpdatePersistenceAdapter,
    private val springDataUserRepository: SpringDataUserRepository
) {

    @Test
    @DisplayName("ACTIVE 유저를 BLOCKED로 차단한다")
    fun block_activeUser_success() {
        // given
        val email = "active@kakao.com"
        springDataUserRepository.save(userEntity(email, UserStatus.ACTIVE))

        // when
        adapter.block(email)

        // then
        val result = springDataUserRepository.findByEmail(email)
        assertThat(result?.status).isEqualTo(UserStatus.BLOCKED)
    }

    @Test
    @DisplayName("존재하지 않는 유저 차단 시 예외가 발생한다")
    fun block_userNotFound_throwsException() {
        assertThatThrownBy { adapter.block("nobody@kakao.com") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("BLOCKED 유저를 ACTIVE로 차단 해제한다")
    fun unblock_blockedUser_success() {
        // given
        val email = "blocked@kakao.com"
        springDataUserRepository.save(userEntity(email, UserStatus.BLOCKED))

        // when
        adapter.unblock(email)

        // then
        val result = springDataUserRepository.findByEmail(email)
        assertThat(result?.status).isEqualTo(UserStatus.ACTIVE)
    }

    @Test
    @DisplayName("존재하지 않는 유저 차단 해제 시 예외가 발생한다")
    fun unblock_userNotFound_throwsException() {
        assertThatThrownBy { adapter.unblock("nobody@kakao.com") }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserErrorCode.USER_NOT_FOUND)
    }

    private fun userEntity(email: String, status: UserStatus) = UserJpaEntity(
        email = email,
        nickname = "테스터",
        provider = OAuth2Provider.KAKAO,
        role = UserRole.NORMAL,
        status = status
    )
}
