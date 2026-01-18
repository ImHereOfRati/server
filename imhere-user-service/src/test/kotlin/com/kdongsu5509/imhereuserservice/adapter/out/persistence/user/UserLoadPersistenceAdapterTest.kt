package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.UserMapper
import com.kdongsu5509.imhereuserservice.domain.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.User
import com.kdongsu5509.imhereuserservice.domain.UserRole
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class UserLoadPersistenceAdapterTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Mock
    lateinit var springQueryDSLUserRepository: SpringQueryDSLUserRepository

    @InjectMocks
    lateinit var userLoadPersistenceAdapter: UserLoadPersistenceAdapter

    companion object {
        const val testEmail = "test@test.com"
        const val testNickname = "고동수"
        val testUser = User(testEmail, testNickname, OAuth2Provider.KAKAO, UserRole.NORMAL)
        val testUserEntity = UserJpaEntity(testUser.email, testUser.nickname, testUser.role, OAuth2Provider.KAKAO)
    }

    @Test
    @DisplayName("사용자가 존재하면 잘 찾아온다")
    fun findByEmail_success() {
        //given
        Mockito.`when`(springDataUserRepository.findByEmail(testEmail)).thenReturn(testUserEntity)
        Mockito.`when`(userMapper.mapToDomainEntity(testUserEntity)).thenReturn(testUser)

        //when
        val result = userLoadPersistenceAdapter.findByEmail(testEmail)

        //then
        org.junit.jupiter.api.Assertions.assertEquals(testEmail, result.email)

        Mockito.verify(springDataUserRepository).findByEmail(testEmail)
    }

    @ParameterizedTest
    @ValueSource(strings = [testNickname, testEmail])
    @DisplayName("사용자를 keyword(email or nickname) 으로 잘 찾는다")
    fun findUserByKeyword_success(testKeyword: String) {
        //given
        Mockito.`when`(userMapper.mapToDomainEntity(testUserEntity)).thenReturn(testUser)
        Mockito.`when`(springQueryDSLUserRepository.findUserByKeyword(testKeyword)).thenReturn(
            listOf<UserJpaEntity>(
                testUserEntity
            )
        )

        //when
        val result = userLoadPersistenceAdapter.findByEmailAndNickname(testKeyword)

        //then
        Assertions.assertThat(result[0].nickname).isEqualTo(testNickname)
        Assertions.assertThat(result[0].email).isEqualTo(testEmail)
        Assertions.assertThat(result.size).isEqualTo(1)

        Mockito.verify(springQueryDSLUserRepository).findUserByKeyword(testKeyword)
    }

    @ParameterizedTest
    @ValueSource(strings = ["unknow", "익명", "anonymous"])
    @DisplayName("없는 keyword(email or nickname) 으로 검색하면 사이즈가 0인 빈 리스트가 반환되는데, 이 때도 오류는 발생하지 않는다.")
    fun findUserByKeyword_with_NotExist(testKeyword: String) {
        //given
        Mockito.`when`(springQueryDSLUserRepository.findUserByKeyword(testKeyword)).thenReturn(
            listOf()
        )

        //when
        val result = userLoadPersistenceAdapter.findByEmailAndNickname(testKeyword)

        //then
        Assertions.assertThat(result.size).isEqualTo(0)

        Mockito.verify(springQueryDSLUserRepository).findUserByKeyword(testKeyword)
    }
}