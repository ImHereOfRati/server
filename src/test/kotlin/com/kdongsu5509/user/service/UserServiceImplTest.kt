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
    @DisplayName("이메일로 사용자를 조회하면 성공하고 UserResult를 반환한다")
    fun findByEmail_success() {
        // given
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(testUser)

        // when
        val result = userServiceImpl.findByEmail(TEST_EMAIL)

        // then
        val expected = UserResult(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
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
        val expected = UserResult(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
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
        val expected = UserResult(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        assertThat(result.content[0]).isEqualTo(expected)
        verify(userRepository).findSliceByEmailAndNickname(TEST_EMAIL, TEST_NICKNAME, pageable)
    }

    @Test
    @DisplayName("사용자 닉네임을 변경하고 변경된 UserResult를 반환한다")
    fun updateNickname_success() {
        // given
        val newNickname = "새닉네임"
        val updatedUser = User(
            id = userId,
            email = TEST_EMAIL,
            nickname = newNickname,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        `when`(userRepository.updateNickname(TEST_EMAIL, newNickname)).thenReturn(updatedUser)

        // when
        val result = userServiceImpl.updateNickname(TEST_EMAIL, newNickname)

        // then
        val expected = UserResult(
            id = userId,
            email = TEST_EMAIL,
            nickname = newNickname,
            oauthProvider = OAuth2Provider.KAKAO,
            role = UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        assertThat(result).isEqualTo(expected)
        verify(userRepository).updateNickname(TEST_EMAIL, newNickname)
    }

    @Test
    @DisplayName("사용자를 차단한다")
    fun block_success() {
        // when
        userServiceImpl.block(TEST_EMAIL)

        // then
        verify(userRepository).block(TEST_EMAIL)
    }

    @Test
    @DisplayName("사용자 차단을 해제한다")
    fun unblock_success() {
        // when
        userServiceImpl.unblock(TEST_EMAIL)

        // then
        verify(userRepository).unblock(TEST_EMAIL)
    }
}
