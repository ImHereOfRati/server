package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class UserSavePersistenceAdapterTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Mock
    lateinit var springQueryDSLUserRepository: SpringQueryDSLUserRepository

    @InjectMocks
    lateinit var userSavePersistenceAdapter: UserSavePersistenceAdapter

    companion object {
        const val testEmail = "test@test.com"
        const val testNickname = "고동수"
        val testUser = User(
            UUID.randomUUID(),
            testEmail,
            testNickname,
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
    @DisplayName("사용자를 잘 저장한다")
    fun saveUser() {
        //given
        Mockito.`when`(userMapper.mapToJpaEntity(testUser)).thenReturn(
            testUserEntity
        )
        BDDMockito.given(springDataUserRepository.save(testUserEntity))
            .willReturn(testUserEntity)

        //when
        userSavePersistenceAdapter.save(testUser)

        //then
        Mockito.verify(springDataUserRepository).save(testUserEntity)
    }
}