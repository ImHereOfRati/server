package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.mapper.UserMapper
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import com.kdongsu5509.user.exception.UserError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

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
        const val TEST_OWNER_EMAIL = "owner@owner.com"
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "테스트"
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
    @DisplayName("이메일로 사용자를 조회하면 성공적으로 도메인 모델을 반환한다")
    fun findByEmail_success() {
        // given
        `when`(springQueryDSLUserRepository.findUserByEmail(TEST_EMAIL)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(TEST_EMAIL)
        verify(springQueryDSLUserRepository).findUserByEmail(TEST_EMAIL)
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 예외가 발생한다")
    fun findByEmail_fail() {
        // given
        `when`(springQueryDSLUserRepository.findUserByEmail("none@test.com")).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userLoadPersistenceAdapter.findByEmail("none@test.com")
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(com.kdongsu5509.support.exception.ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("사용자가 존재하고 활성 상태이면 성공적으로 반환한다")
    fun findActiveUserByEmail_success() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(TEST_EMAIL)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findActiveUserByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(TEST_EMAIL)
    }

    @Test
    @DisplayName("활성 사용자가 존재하지 않으면 예외가 발생한다")
    fun findActiveUserByEmail_fail() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(TEST_EMAIL)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userLoadPersistenceAdapter.findActiveUserByEmail(TEST_EMAIL)
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(com.kdongsu5509.support.exception.ErrorReason.NOT_FOUND)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEST_NICKNAME, TEST_EMAIL])
    @DisplayName("키워드(이메일 또는 닉네임)로 검색하면 해당 사용자 리스트를 반환한다")
    fun findPotentialFriendsByEmailAndNickname_success(testKeyword: String) {
        // given
        `when`(springQueryDSLUserRepository.searchNewFriendCandidates(TEST_OWNER_EMAIL, testKeyword)).thenReturn(
            listOf(
                testUserEntity
            )
        )
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userLoadPersistenceAdapter.findPotentialFriendsByEmailAndNickname(TEST_OWNER_EMAIL, testKeyword)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].email).isEqualTo(TEST_EMAIL)
        verify(springQueryDSLUserRepository).searchNewFriendCandidates(TEST_OWNER_EMAIL, testKeyword)
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트를 반환한다")
    fun findPotentialFriendsByEmailAndNickname_empty() {
        // given
        val keyword = "not_found"
        `when`(
            springQueryDSLUserRepository.searchNewFriendCandidates(
                TEST_OWNER_EMAIL,
                keyword
            )
        ).thenReturn(emptyList())

        // when
        val result = userLoadPersistenceAdapter.findPotentialFriendsByEmailAndNickname(TEST_OWNER_EMAIL, keyword)

        // then
        assertThat(result).isEmpty()
    }
}
