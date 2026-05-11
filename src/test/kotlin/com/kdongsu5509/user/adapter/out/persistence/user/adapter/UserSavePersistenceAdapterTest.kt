package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserSavePersistenceAdapterTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @InjectMocks
    lateinit var userSavePersistenceAdapter: UserSavePersistenceAdapter

    companion object {
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "고동수"
        val testUser = User(
            UUID.randomUUID(),
            TEST_EMAIL,
            TEST_NICKNAME,
            OAuth2Provider.KAKAO,
            UserRole.NORMAL,
            status = UserStatus.ACTIVE
        )
        val testUserEntity = UserJpaEntity(
            testUser.email,
            testUser.nickname,
            testUser.role,
            OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
    }

    @Test
    @DisplayName("사용자를 성공적으로 저장한다")
    fun save_success() {
        // given
        given(userMapper.toJpaEntity(testUser)).willReturn(testUserEntity)
        given(springDataUserRepository.save(testUserEntity)).willReturn(testUserEntity)

        // when
        userSavePersistenceAdapter.save(testUser)

        // then
        verify(springDataUserRepository).save(testUserEntity)
    }
}
