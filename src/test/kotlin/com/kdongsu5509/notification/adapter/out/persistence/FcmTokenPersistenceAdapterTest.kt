package com.kdongsu5509.imhere.notification.adapter.out.persistence

import com.kdongsu5509.imhere.auth.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhere.auth.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhere.auth.domain.OAuth2Provider
import com.kdongsu5509.imhere.auth.domain.UserRole
import com.kdongsu5509.imhere.common.exception.domain.auth.UserNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FcmTokenPersistenceAdapterTest {

    @Mock
    private lateinit var springDataFcmTokenRepository: SpringDataFcmTokenRepository

    @Mock
    private lateinit var springDataUserRepository: SpringDataUserRepository

    private val fcmTokenMapper = FcmTokenMapper()

    private lateinit var fcmTokenPersistenceAdapter: FcmTokenPersistenceAdapter

    @BeforeEach
    fun setUp() {
        fcmTokenPersistenceAdapter = FcmTokenPersistenceAdapter(
            fcmTokenMapper = fcmTokenMapper,
            springDataFcmTokenRepository = springDataFcmTokenRepository,
            springDataUserRepository = springDataUserRepository
        )
    }

    @Test
    @DisplayName("이미 저장되어 있는 토큰이 있고, 변경되지 않았으면 아무것도 하지 않는다")
    fun save_do_nothing_when_already_exist_and_not_change() {
        //given
        val testEmail = "dongsu@test.com"
        val testFcmToken = "testFcmToken"
        val testUserJpaEntity = createUserJpaEntity(testEmail)
        val testFcmTokenEntity = FcmTokenEntity(
            testFcmToken,
            testUserJpaEntity
        )

        `when`(springDataFcmTokenRepository.findByUserEmail(testEmail)).thenReturn(testFcmTokenEntity)

        //when
        fcmTokenPersistenceAdapter.save(testFcmToken, testEmail)

        //then
        assertEquals(testFcmToken, testFcmTokenEntity.token)
        verify(springDataFcmTokenRepository, times(0)).save(any())
    }

    @Test
    @DisplayName("이미 저장된 토큰이 있지만, 입력된 토큰과 다르면 엔티티를 업데이트한다")
    fun save_update_token_when_different() {
        // given
        val testEmail = "dongsu@test.com"
        val oldToken = "oldToken"
        val newToken = "newToken"
        val testUser = createUserJpaEntity(testEmail)

        val existingEntity = FcmTokenEntity(oldToken, testUser)

        `when`(springDataFcmTokenRepository.findByUserEmail(testEmail)).thenReturn(existingEntity)

        // when
        fcmTokenPersistenceAdapter.save(newToken, testEmail)

        // then
        assertEquals(newToken, existingEntity.token)
    }

    @Test
    @DisplayName("저장된 토큰이 없으면 새로 생성하여 저장한다")
    fun save_new_token_when_not_exist() {
        // given
        val testEmail = "dongsu@test.com"
        val newToken = "newToken"
        val testUser = createUserJpaEntity(testEmail)

        `when`(springDataFcmTokenRepository.findByUserEmail(testEmail)).thenReturn(null)
        `when`(springDataUserRepository.findByEmail(testEmail)).thenReturn(testUser)
        `when`(springDataFcmTokenRepository.save(any(FcmTokenEntity::class.java)))
            .thenAnswer { it.arguments[0] }

        // when
        fcmTokenPersistenceAdapter.save(newToken, testEmail)

        // then
        verify(springDataFcmTokenRepository, times(1)).save(any(FcmTokenEntity::class.java))
    }

    @Test
    @DisplayName("토큰도 없고 유저 정보도 없으면 UserNotFoundException이 발생한다")
    fun save_fail_when_user_not_found() {
        // given
        val testEmail = "unknown@test.com"
        val newToken = "token"

        `when`(springDataFcmTokenRepository.findByUserEmail(testEmail)).thenReturn(null)
        `when`(springDataUserRepository.findByEmail(testEmail)).thenReturn(null)

        // when & then
        assertThrows<UserNotFoundException> {
            fcmTokenPersistenceAdapter.save(newToken, testEmail)
        }
    }

    @Test
    @DisplayName("유저 이메일로 토큰을 조회하면 도메인 객체로 매핑되어 반환된다")
    fun find_by_email_success() {
        // given
        val testEmail = "dongsu@test.com"
        val token = "testToken"
        val testUser = createUserJpaEntity(testEmail)
        val entity = FcmTokenEntity(token, testUser)

        `when`(springDataFcmTokenRepository.findByUserEmail(testEmail)).thenReturn(entity)

        // when
        val result = fcmTokenPersistenceAdapter.findByUserEmail(testEmail)

        // then
        assertEquals(token, result?.fcmToken)
        assertEquals(testEmail, result?.userEmail)
    }

    private fun createUserJpaEntity(email: String): UserJpaEntity {
        return UserJpaEntity(
            email = email,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO
        )
    }
}