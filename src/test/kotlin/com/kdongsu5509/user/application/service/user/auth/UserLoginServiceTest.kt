package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.port.out.user.UserLoadPort
import com.kdongsu5509.user.application.port.out.user.UserSavePort
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenProviderPort
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserLoginServiceTest {

    companion object {
        private const val EMAIL = "user@example.com"
        private const val NICKNAME = "홍길동"
        private val PROVIDER = OAuth2Provider.KAKAO
        private val JWT = ImHereJwt("access-token", "refresh-token")
    }

    @Mock
    private lateinit var userLoadPort: UserLoadPort

    @Mock
    private lateinit var userSavePort: UserSavePort

    @Mock
    private lateinit var tokenProvider: ImHereTokenProviderPort

    private lateinit var loginService: UserLoginService

    @BeforeEach
    fun setUp() {
        loginService = UserLoginService(userLoadPort, userSavePort, tokenProvider)
    }

    @Test
    @DisplayName("기존 유저가 로그인하면 추가 저장 없이 토큰을 발급한다")
    fun login_existing_user_success() {
        // given
        val user = createTestUser(UserStatus.ACTIVE)
        given(userLoadPort.findByEmail(EMAIL)).willReturn(user)
        givenTokenIssuanceSucceeds()

        // when
        val result = loginService.loginOrRegister(EMAIL, NICKNAME, PROVIDER)

        // then
        assertThat(result.isNewUser).isFalse()
        assertThat(result.accessToken).isEqualTo(JWT.accessToken)

        then(userSavePort).shouldHaveNoInteractions()
        then(tokenProvider).should().issue(any())
    }

    @Test
    @DisplayName("신규 유저가 로그인 시도 시 회원가입을 진행하고 토큰을 발급한다")
    fun login_new_user_success() {
        // given
        val newUser = createTestUser(UserStatus.PENDING)
        given(userLoadPort.findByEmail(EMAIL)).willReturn(null)
        given(userSavePort.save(any())).willReturn(newUser)
        givenTokenIssuanceSucceeds()

        // when
        val result = loginService.loginOrRegister(EMAIL, NICKNAME, PROVIDER)

        // then
        assertThat(result.isNewUser).isTrue()
        assertThat(result.accessToken).isEqualTo(JWT.accessToken)

        then(userSavePort).should().save(any())
        then(tokenProvider).should().issue(any())
    }

    private fun createTestUser(status: UserStatus): User {
        return User(UUID.randomUUID(), EMAIL, NICKNAME, PROVIDER, UserRole.NORMAL, status)
    }

    private fun givenTokenIssuanceSucceeds() {
        given(tokenProvider.issue(any())).willReturn(JWT)
    }
}
