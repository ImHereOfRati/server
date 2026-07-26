package com.kdongsu5509.user.event

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserForceLogoutEventListenerTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var listener: UserForceLogoutEventListener

    @Test
    @DisplayName("강제 로그아웃 이벤트를 받으면 Refresh Token 버전을 증가시켜 저장한다")
    fun force_logout_event_increments_refresh_token_version() {
        val email = "target@test.com"
        val user = user(email)
        whenever(userRepository.findByEmail(email)).thenReturn(user)

        listener.handle(UserForceLogoutEvent(email))

        Mockito.verify(userRepository).update(user.rotateRefreshTokenVersion())
    }

    @Test
    @DisplayName("강제 로그아웃 대상 사용자가 없으면 이벤트 처리를 종료한다")
    fun force_logout_event_ignores_missing_user() {
        val email = "missing@test.com"
        whenever(userRepository.findByEmail(email)).thenReturn(null)

        listener.handle(UserForceLogoutEvent(email))

        Mockito.verify(userRepository, never()).update(any())
    }

    private fun user(email: String) = User(
        id = UUID.randomUUID(),
        email = email,
        nickname = "tester",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE,
    )
}
