package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserRepositoryImplTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var entityManager: EntityManager

    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Mock
    lateinit var springQueryDSLUserRepository: SpringQueryDSLUserRepository

    @InjectMocks
    lateinit var userRepositoryImpl: UserRepositoryImpl

    companion object {
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "테스트"
        val testUser = User(
            id = UUID.randomUUID(),
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
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
    @DisplayName("아이디로 사용자를 조회하면 성공하고 유저 모델을 반환한다")
    fun findById_success() {
        // given
        val testOptionalUserEntity = Optional.of(testUserEntity)
        `when`(springDataUserRepository.findById(testUser.id!!)).thenReturn(testOptionalUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.findById(testUser.id)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(TEST_EMAIL)
        verify(springDataUserRepository).findById(testUser.id)
    }

    @Test
    @DisplayName("아이디로 사용자를 조회 시 존재하지 않으면 null을 반환한다")
    fun findById_null() {
        // given
        val notExistUserId = UUID.randomUUID()
        `when`(springDataUserRepository.findById(notExistUserId)).thenReturn(Optional.empty())

        // when
        val result = userRepositoryImpl.findById(notExistUserId)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("이메일로 사용자를 조회하면 성공하고 유저 모델을 반환한다")
    fun findByEmail_success() {
        // given
        `when`(springDataUserRepository.findByEmail(TEST_EMAIL)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.findByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(TEST_EMAIL)
        verify(springDataUserRepository).findByEmail(TEST_EMAIL)
    }

    @Test
    @DisplayName("이메일로 사용자를 조회 시 존재하지 않으면 null을 반환한다")
    fun findByEmail_null() {
        // given
        `when`(springDataUserRepository.findByEmail(TEST_EMAIL)).thenReturn(null)

        // when
        val result = userRepositoryImpl.findByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("전체 사용자를 슬라이스 형태로 조회한다")
    fun findAll_success() {
        // given
        val pageable = PageRequest.of(0, 20)
        val slice = SliceImpl(listOf(testUserEntity), pageable, false)
        `when`(springQueryDSLUserRepository.findAll(pageable)).thenReturn(slice)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.findAll(pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].email).isEqualTo(TEST_EMAIL)
    }

    @Test
    @DisplayName("사용자가 존재하고 활성 상태면 활성 사용자를 성공으로 반환한다")
    fun findActiveUserByEmail_success() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(TEST_EMAIL)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.findActiveUserByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNotNull
        assertThat(result?.email).isEqualTo(TEST_EMAIL)
    }

    @Test
    @DisplayName("활성 사용자가 존재하지 않으면 null을 반환한다")
    fun findActiveUserByEmail_null() {
        // given
        `when`(springQueryDSLUserRepository.findActiveUserByEmail(TEST_EMAIL)).thenReturn(null)

        // when
        val result = userRepositoryImpl.findActiveUserByEmail(TEST_EMAIL)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("키워드가 이메일 또는 닉네임과 일치하면 해당하는 유저 슬라이스를 반환한다")
    fun findSliceByEmailAndNickname_success() {
        // given
        val pageable = PageRequest.of(0, 20)
        `when`(
            springQueryDSLUserRepository.findAllActiveByEmailAndKeyword(
                "owner@owner.com",
                TEST_NICKNAME,
                pageable
            )
        ).thenReturn(SliceImpl(listOf(testUserEntity), pageable, false))
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.findSliceByEmailAndNickname("owner@owner.com", TEST_NICKNAME, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].email).isEqualTo(TEST_EMAIL)
    }

    @Test
    @DisplayName("사용자 정보를 저장하고 도메인 객체를 반환한다")
    fun save_success() {
        // given
        `when`(userMapper.toEntity(testUser)).thenReturn(testUserEntity)
        `when`(springDataUserRepository.save(testUserEntity)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userRepositoryImpl.save(testUser)

        // then
        assertThat(result).isEqualTo(testUser)
        verify(springDataUserRepository).save(testUserEntity)
    }

    @Test
    @DisplayName("사용자 정보 수정 요청 시 영속화된 엔티티의 데이터를 업데이트한다")
    fun update_success() {
        // given
        val userId = testUser.id!!
        val existingJpaEntity = UserJpaEntity(
            TEST_EMAIL,
            TEST_NICKNAME,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            status = UserStatus.PENDING
        )
        `when`(entityManager.find(UserJpaEntity::class.java, userId)).thenReturn(existingJpaEntity)

        // 도메인 레이어에서 변경이 일어난 유저 상태 시뮬레이션 (ACTIVE 상태 및 새 닉네임)
        val updatedDomainUser = User(
            id = userId,
            email = TEST_EMAIL,
            nickname = "newNickname",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        // when
        userRepositoryImpl.update(updatedDomainUser)

        // then
        // 구현체의 userJpaEntity.update(user) 내부 로직이 올바르게 수행되었는지 엔티티 상태로 검증
        assertThat(existingJpaEntity.status).isEqualTo(UserStatus.ACTIVE)
        assertThat(existingJpaEntity.nickname).isEqualTo("newNickname")
    }

    @Test
    @DisplayName("존재하지 않는 사용자 정보로 수정 시도 시 예외가 발생한다")
    fun update_fail_when_user_not_found() {
        // given
        val notExistUserId = UUID.randomUUID()
        val dummyUser = User(
            id = notExistUserId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        `when`(entityManager.find(UserJpaEntity::class.java, notExistUserId)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userRepositoryImpl.update(dummyUser)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("사용자 이메일 기반으로 존재 및 미존재 여부를 확인한다")
    fun existByEmail() {
        // given
        val notExistUserId = UUID.randomUUID()
        val dummyUser = User(
            id = notExistUserId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        given(springDataUserRepository.existsByEmail(TEST_EMAIL)).willReturn(true)
        given(springDataUserRepository.existsByEmail("noexist@gmail.com")).willReturn(false)

        // when
        val trueResult = userRepositoryImpl.existsByEmail(TEST_EMAIL)
        val falseResult = userRepositoryImpl.existsByEmail("noexist@gmail.com")

        // then
        assertThat(trueResult).isEqualTo(true)
        assertThat(falseResult).isEqualTo(false)
    }
}
