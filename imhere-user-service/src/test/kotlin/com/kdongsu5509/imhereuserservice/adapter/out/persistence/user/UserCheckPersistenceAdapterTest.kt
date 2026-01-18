package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.User
import com.kdongsu5509.imhereuserservice.domain.UserRole
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class UserCheckPersistenceAdapterTest {
    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @InjectMocks
    lateinit var userCheckPersistenceAdapter: UserCheckPersistenceAdapter

    companion object {
        const val testEmail = "test@test.com"
        const val testNickname = "고동수"
        val testUser = User(testEmail, testNickname, OAuth2Provider.KAKAO, UserRole.NORMAL)
        val testUserEntity = UserJpaEntity(testUser.email, testUser.nickname, testUser.role, OAuth2Provider.KAKAO)
    }

    @Test
    @DisplayName("존재하는 이메일은 true을 반환한다")
    fun existByEmail_success() {
        //given
        val email = "test@test.com"
        Mockito.`when`(springDataUserRepository.existsByEmail(email)).thenReturn(true)

        //when
        val result = userCheckPersistenceAdapter.existsByEmail(email)

        //then
        Assertions.assertThat(result).isTrue
    }

    @Test
    @DisplayName("존재하지 않는 이메일이라고 확인되면 false를 반환한다")
    fun existByEmail_fail() {
        //given
        Mockito.`when`(springDataUserRepository.existsByEmail(testEmail)).thenReturn(false)

        //when
        val result = userCheckPersistenceAdapter.existsByEmail(testEmail)

        //then
        Assertions.assertThat(result).isFalse
    }
}