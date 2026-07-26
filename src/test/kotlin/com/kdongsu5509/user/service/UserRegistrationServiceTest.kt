package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.api.RegisterUserCommand
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
class UserRegistrationServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userRegistrationService: UserRegistrationService

    private val command = RegisterUserCommand(
        email = "new@example.com",
        nickname = "신규사용자",
        oauthProvider = OAuth2Provider.GOOGLE,
        oidcSubject = "google-subject",
    )

    @Test
    @DisplayName("중복 이메일이 없으면 PENDING 사용자를 생성하여 저장한다")
    fun register_saves_new_pending_user() {
        val savedUser = User(
            id = UUID.randomUUID(),
            email = command.email,
            nickname = command.nickname,
            role = UserRole.NORMAL,
            oauthProvider = command.oauthProvider,
            status = UserStatus.PENDING,
            oidcSubject = command.oidcSubject,
        )
        whenever(userRepository.findByEmail(command.email)).thenReturn(null)
        whenever(userRepository.save(any())).thenReturn(savedUser)

        val result = userRegistrationService.register(command)

        assertThat(result.id).isEqualTo(savedUser.id)
        assertThat(result.status).isEqualTo(UserStatus.PENDING)
        verify(userRepository).save(
            User(
                email = command.email,
                nickname = command.nickname,
                oauthProvider = command.oauthProvider,
                oidcSubject = command.oidcSubject,
            )
        )
    }

    @Test
    @DisplayName("이미 등록된 이메일이면 신규 사용자를 저장하지 않고 예외를 발생시킨다")
    fun register_rejects_duplicate_email() {
        val existingUser = User(
            id = UUID.randomUUID(),
            email = command.email,
            nickname = "기존사용자",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE,
        )
        whenever(userRepository.findByEmail(command.email)).thenReturn(existingUser)

        assertThatThrownBy { userRegistrationService.register(command) }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.DUPLICATE_EMAIL)
        verify(userRepository, never()).save(any())
    }
}
