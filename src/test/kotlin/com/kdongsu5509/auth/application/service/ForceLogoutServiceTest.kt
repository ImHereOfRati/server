package com.kdongsu5509.auth.application.service

import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ForceLogoutServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private val forceLogoutService by lazy { ForceLogoutService(userRepository) }

    @Test
    @DisplayName("force logout 시 refresh token version을 올린다")
    fun logout_incrementsRefreshTokenVersion() {
        val user = User(
            id = UUID.randomUUID(),
            email = "test@example.com",
            nickname = "tester",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE,
            refreshTokenVersion = 0
        )

        given(userRepository.findByEmail(user.email)).willReturn(user)

        forceLogoutService.logout(user.email)

        then(userRepository).should().update(user.rotateRefreshTokenVersion())
    }

    @Test
    @DisplayName("존재하지 않는 사용자는 무시한다")
    fun logout_ignoresMissingUser() {
        given(userRepository.findByEmail("missing@example.com")).willReturn(null)

        forceLogoutService.logout("missing@example.com")

        then(userRepository).should().findByEmail("missing@example.com")
        verifyNoMoreInteractions(userRepository)
    }
}
