package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserProfileServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userProfileService: UserProfileService

    private val email = "test@test.com"
    private val user = User(
        id = UUID.randomUUID(),
        email = email,
        nickname = "tester",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE,
    )

    @Test
    @DisplayName("닉네임을 변경하면 변경된 사용자 정보를 저장한다")
    fun updateNickname_persists_changed_user() {
        whenever(userRepository.findByEmail(email)).thenReturn(user)

        val result = userProfileService.updateNickname(email, "changed")

        assertThat(result.nickname).isEqualTo("changed")
        verify(userRepository).update(any())
    }

    @Test
    @DisplayName("존재하지 않는 사용자의 닉네임을 변경하면 예외가 발생한다")
    fun updateNickname_throws_when_user_does_not_exist() {
        whenever(userRepository.findByEmail(email)).thenReturn(null)

        assertThatThrownBy { userProfileService.updateNickname(email, "changed") }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
        verify(userRepository, never()).update(any())
    }
}
