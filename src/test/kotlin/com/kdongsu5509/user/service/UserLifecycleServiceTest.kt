package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.event.UserForceLogoutEvent
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.shared.event.UserWithdrawnEvent
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
import org.springframework.context.ApplicationEventPublisher
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UserLifecycleServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var eventPublisher: ApplicationEventPublisher

    @Mock
    lateinit var domainEventPublisher: DomainEventPublisher


    @InjectMocks
    lateinit var userLifecycleService: UserLifecycleService

    private val userId = UUID.randomUUID()
    private val email = "test@test.com"

    private fun user(status: UserStatus = UserStatus.ACTIVE) = User(
        id = userId,
        email = email,
        nickname = "tester",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = status,
    )

    @Test
    @DisplayName("존재하지 않는 사용자를 활성화하면 예외가 발생한다")
    fun activateIfPending_throws_when_user_does_not_exist() {
        whenever(userRepository.findById(userId)).thenReturn(null)

        assertUserNotFound { userLifecycleService.activateIfPending(userId) }
        verify(userRepository, never()).update(any())
    }

    @Test
    @DisplayName("PENDING 사용자에게 필수 약관 동의 이벤트가 도착하면 활성화한다")
    fun activateIfPending_activates_pending_user() {
        whenever(userRepository.findById(userId)).thenReturn(user(UserStatus.PENDING))

        val result = userLifecycleService.activateIfPending(userId)

        assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
        verify(userRepository).update(any())
    }

    @Test
    @DisplayName("PENDING이 아닌 사용자에게 필수 약관 동의 이벤트가 도착하면 상태를 변경하지 않는다")
    fun activateIfPending_does_not_update_non_pending_user() {
        whenever(userRepository.findById(userId)).thenReturn(user(UserStatus.ACTIVE))

        val result = userLifecycleService.activateIfPending(userId)

        assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
        verify(userRepository, never()).update(any())
    }

    @Test
    @DisplayName("사용자를 차단하면 BLOCKED 상태를 저장하고 강제 로그아웃 이벤트를 발행한다")
    fun block_persists_status_and_publishes_force_logout_event() {
        whenever(userRepository.findByEmail(email)).thenReturn(user())

        val result = userLifecycleService.block(email)

        assertThat(result.status).isEqualTo(UserStatus.BLOCKED)
        verify(userRepository).update(any())
        verify(eventPublisher).publishEvent(UserForceLogoutEvent(email))
    }

    @Test
    @DisplayName("차단된 사용자를 차단 해제하면 ACTIVE 상태를 저장하고 강제 로그아웃하지 않는다")
    fun unblock_persists_active_status_without_force_logout() {
        whenever(userRepository.findByEmail(email)).thenReturn(user(UserStatus.BLOCKED))

        val result = userLifecycleService.unblock(email)

        assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
        verify(userRepository).update(any())
        verify(eventPublisher, never()).publishEvent(any<Any>())
    }

    @Test
    @DisplayName("활성 사용자가 탈퇴하면 WITHDRAWN 상태를 저장하고 강제 로그아웃 이벤트를 발행한다")
    fun withdraw_persists_status_and_publishes_force_logout_event() {
        whenever(userRepository.findByEmail(email)).thenReturn(user())

        val result = userLifecycleService.withdraw(email)

        assertThat(result.status).isEqualTo(UserStatus.WITHDRAWN)
        verify(userRepository).update(any())
        verify(domainEventPublisher).publish(any<UserWithdrawnEvent>())
        verify(eventPublisher).publishEvent(UserForceLogoutEvent(email))
    }

    @Test
    @DisplayName("차단된 사용자가 탈퇴하면 WITHDRAWN 상태를 저장하고 강제 로그아웃 이벤트를 발행한다")
    fun blocked_user_can_withdraw() {
        whenever(userRepository.findByEmail(email)).thenReturn(user(UserStatus.BLOCKED))

        val result = userLifecycleService.withdraw(email)

        assertThat(result.status).isEqualTo(UserStatus.WITHDRAWN)
        verify(userRepository).update(any())
        verify(domainEventPublisher).publish(any<UserWithdrawnEvent>())
        verify(eventPublisher).publishEvent(UserForceLogoutEvent(email))
    }

    @Test
    @DisplayName("강제 로그아웃을 요청하면 대상 이메일을 포함한 이벤트를 발행한다")
    fun requestForceLogout_publishes_event_with_target_email() {
        userLifecycleService.requestForceLogout(email)

        verify(eventPublisher).publishEvent(UserForceLogoutEvent(email))
    }

    private fun assertUserNotFound(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }
}
