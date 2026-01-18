package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserMapper
import com.kdongsu5509.imhereuserservice.domain.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.User
import com.kdongsu5509.imhereuserservice.domain.UserRole
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
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
        val testUser = User(testEmail, testNickname, OAuth2Provider.KAKAO, UserRole.NORMAL)
        val testUserEntity = UserJpaEntity(testUser.email, testUser.nickname, testUser.role, OAuth2Provider.KAKAO)
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