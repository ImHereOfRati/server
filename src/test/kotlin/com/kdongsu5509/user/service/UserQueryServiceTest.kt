package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.api.UserResult
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
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserQueryServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @InjectMocks
    lateinit var userQueryService: UserQueryService

    private val testUserId = UUID.randomUUID()
    private val testEmail = "test@test.com"
    private val testUser = User(
        id = testUserId,
        email = testEmail,
        nickname = "tester",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE,
    )

    @Test
    @DisplayName("아이디로 사용자를 조회하면 사용자 결과를 반환한다")
    fun findById_returns_user() {
        whenever(userRepository.findById(testUserId)).thenReturn(testUser)

        val result = userQueryService.findById(testUserId)

        assertThat(result).isEqualTo(UserResult.fromDomain(testUser))
    }

    @Test
    @DisplayName("존재하지 않는 사용자 아이디로 조회하면 예외가 발생한다")
    fun findById_throws_when_user_does_not_exist() {
        whenever(userRepository.findById(testUserId)).thenReturn(null)

        assertUserNotFound { userQueryService.findById(testUserId) }
    }

    @Test
    @DisplayName("nullable 이메일 조회 시 사용자가 존재하면 사용자 결과를 반환한다")
    fun findByEmailOrNull_returns_user_when_present() {
        whenever(userRepository.findByEmail(testEmail)).thenReturn(testUser)

        val result = userQueryService.findByEmailOrNull(testEmail)

        assertThat(result).isEqualTo(UserResult.fromDomain(testUser))
    }

    @Test
    @DisplayName("nullable 이메일 조회 시 사용자가 없으면 null을 반환한다")
    fun findByEmailOrNull_returns_null_when_missing() {
        whenever(userRepository.findByEmail(testEmail)).thenReturn(null)

        val result = userQueryService.findByEmailOrNull(testEmail)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("전체 사용자를 조회하면 도메인 객체를 사용자 결과로 변환한다")
    fun findAll_maps_users_to_results() {
        val pageable = PageRequest.of(0, 20)
        whenever(userRepository.findAll(pageable))
            .thenReturn(SliceImpl(listOf(testUser), pageable, false))

        val result = userQueryService.findAll(pageable)

        assertThat(result.content).containsExactly(UserResult.fromDomain(testUser))
    }

    @Test
    @DisplayName("키워드로 사용자를 조회하면 도메인 객체를 사용자 결과로 변환한다")
    fun searchActiveByKeyword_maps_users_to_results() {
        val pageable = PageRequest.of(0, 20)
        whenever(userRepository.searchActiveByKeyword("test", emptySet(), pageable))
            .thenReturn(SliceImpl(listOf(testUser), pageable, false))

        val result = userQueryService.searchActiveByKeyword("test", emptySet(), pageable)

        assertThat(result.content).containsExactly(UserResult.fromDomain(testUser))
    }

    private fun assertUserNotFound(action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }
}
