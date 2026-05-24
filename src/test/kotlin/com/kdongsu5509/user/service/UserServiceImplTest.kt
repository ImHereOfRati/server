package com.kdongsu5509.user.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.dto.UserResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserServiceImplTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userServiceImpl: UserServiceImpl

    companion object {
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "테스트"
        val userId = UUID.randomUUID()
        val testUser = User(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
    }

    @Test
    @DisplayName("아이디로 사용자를 조회하면 성공하고 UserResult를 반환한다")
    fun findById_success() {
        // given
        `when`(userRepository.findById(userId)).thenReturn(testUser)

        // when
        val result = userServiceImpl.findById(userId)

        // then
        val expected = UserResult.fromDomain(testUser)
        assertThat(result).isEqualTo(expected)
        verify(userRepository).findById(userId)
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 사용자 조회 시 예외가 발생한다")
    fun findById_fail_user_not_found() {
        // given
        `when`(userRepository.findById(userId)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userServiceImpl.findById(userId)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("이메일로 사용자를 조회하면 성공하고 UserResult를 반환한다")
    fun findByEmail_success() {
        // given
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser)

        // when
        val result = userServiceImpl.findByEmail(TEST_EMAIL)

        // then
        val expected = UserResult.fromDomain(testUser)
        assertThat(result).isEqualTo(expected)
        verify(userRepository).findByEmail(TEST_EMAIL)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 사용자 조회 시 예외가 발생한다")
    fun findByEmail_fail_user_not_found() {
        // given
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userServiceImpl.findByEmail(TEST_EMAIL)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("전체 사용자를 슬라이스 형태로 조회한다")
    fun findAll_success() {
        // given
        val pageable = PageRequest.of(0, 20)
        val slice = SliceImpl(listOf(testUser), pageable, false)
        `when`(userRepository.findAll(pageable)).thenReturn(slice)

        // when
        val result = userServiceImpl.findAll(pageable)

        // then
        assertThat(result.content).hasSize(1)
        val expected = UserResult.fromDomain(testUser)
        assertThat(result.content[0]).isEqualTo(expected)
        verify(userRepository).findAll(pageable)
    }

    @Test
    @DisplayName("키워드로 사용자를 조회하여 슬라이스 형태로 반환한다")
    fun findByKeyword_success() {
        // given
        val pageable = PageRequest.of(0, 20)
        val slice = SliceImpl(listOf(testUser), pageable, false)
        `when`(userRepository.findSliceByEmailAndNickname(TEST_EMAIL, TEST_NICKNAME, pageable)).thenReturn(slice)

        // when
        val result = userServiceImpl.findByKeyword(TEST_EMAIL, TEST_NICKNAME, pageable)

        // then
        assertThat(result.content).hasSize(1)
        val expected = UserResult.fromDomain(testUser)
        assertThat(result.content[0]).isEqualTo(expected)
        verify(userRepository).findSliceByEmailAndNickname(TEST_EMAIL, TEST_NICKNAME, pageable)
    }

    @Test
    @DisplayName("사용자 닉네임을 변경하고 변경된 UserResult를 반환한다")
    fun updateNickname_success() {
        // given
        val newNickname = "새닉네임"
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser)

        // when
        val result = userServiceImpl.updateNickname(TEST_EMAIL, newNickname)

        // then
        assertThat(result.nickname).isEqualTo(newNickname)
        verify(userRepository).findByEmail(TEST_EMAIL)
        verify(userRepository).update(any())
    }

    @Test
    @DisplayName("사용자를 차단 상태로 변경하고 수정 요청을 보낸다")
    fun block_success() {
        // given
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser)

        // when
        val result = userServiceImpl.block(TEST_EMAIL)

        // then
        assertThat(result.status).isEqualTo(UserStatus.BLOCKED)
        verify(userRepository).findByEmail(TEST_EMAIL)
        verify(userRepository).update(any())
    }

    @Test
    @DisplayName("사용자 차단을 해제하여 활성 상태로 변경하고 수정 요청을 보낸다")
    fun unblock_success() {
        // given
        val blockedUser = User(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.BLOCKED
        )
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(blockedUser)

        // when
        val result = userServiceImpl.unblock(TEST_EMAIL)

        // then
        assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
        verify(userRepository).findByEmail(TEST_EMAIL)
        verify(userRepository).update(any())
    }
}
