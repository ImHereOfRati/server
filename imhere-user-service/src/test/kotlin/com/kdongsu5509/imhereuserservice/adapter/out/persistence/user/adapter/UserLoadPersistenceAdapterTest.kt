package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class UserLoadPersistenceAdapterTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var springQueryDSLUserRepository: SpringQueryDSLUserRepository

    @InjectMocks
    lateinit var userLoadPersistenceAdapter: UserLoadPersistenceAdapter

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
    @DisplayName("이메일로 사용자를 조회하면 성공적으로 도메인 모델을 반환한다")
    fun findUserByEmailOrNull_success() {
        // given
        `when`(springQueryDSLUserRepository.findUserByEmail(testEmail)).thenReturn(Optional.of(testUserEntity))
        `when`(userMapper.mapToDomainEntity(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findUserByEmailOrNull(testEmail)

        // then
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result?.email).isEqualTo(testEmail)
        verify(springQueryDSLUserRepository).findUserByEmail(testEmail)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
    fun findUserByEmailOrNull_fail() {
        // given
        `when`(springQueryDSLUserRepository.findUserByEmail("none@test.com")).thenReturn(Optional.empty())

        // when
        val result = userLoadPersistenceAdapter.findUserByEmailOrNull("none@test.com")

        // then
        Assertions.assertThat(result).isNull()
    }

    @Test
    @DisplayName("사용자가 존재하고 활성 상태이면 성공적으로 반환한다")
    fun findActiveUserByEmailOrNull_success() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(testEmail)).thenReturn(Optional.of(testUserEntity))
        `when`(userMapper.mapToDomainEntity(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findActiveUserByEmailOrNull(testEmail)

        // then
        Assertions.assertThat(result).isNotNull
        Assertions.assertThat(result?.email).isEqualTo(testEmail)
    }

    @Test
    @DisplayName("활성 사용자가 존재하지 않으면 null을 반환한다")
    fun findActiveUserByEmailOrNull_fail() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(testEmail)).thenReturn(Optional.empty())

        // when
        val result = userLoadPersistenceAdapter.findActiveUserByEmailOrNull(testEmail)

        // then
        Assertions.assertThat(result).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = [testNickname, testEmail])
    @DisplayName("키워드(이메일 또는 닉네임)로 검색 시 해당 사용자 리스트를 반환한다")
    fun findUserByKeyword_success(testKeyword: String) {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByKeyword(testKeyword)).thenReturn(listOf(testUserEntity))
        `when`(userMapper.mapToDomainEntity(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findByEmailAndNickname(testKeyword)

        // then
        Assertions.assertThat(result).hasSize(1)
        Assertions.assertThat(result[0].email).isEqualTo(testEmail)
        verify(springQueryDSLUserRepository).findActiveUserByKeyword(testKeyword)
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
    fun findUserByKeyword_empty() {
        // given
        val keyword = "not_found"
        `when`(springQueryDSLUserRepository.findActiveUserByKeyword(keyword)).thenReturn(emptyList())

        // when
        val result = userLoadPersistenceAdapter.findByEmailAndNickname(keyword)

        // then
        Assertions.assertThat(result).isEmpty()
    }
}