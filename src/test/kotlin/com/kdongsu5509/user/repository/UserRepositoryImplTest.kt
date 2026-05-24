package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
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
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserRepositoryImplTest {
    @Mock
    lateinit var userMapper: UserMapper

    @Mock
    lateinit var springDataUserRepository: SpringDataUserRepository

    @Mock
    lateinit var springQueryDSLUserRepository: SpringQueryDSLUserRepository

    @InjectMocks
    lateinit var userDaoImpl: UserRepositoryImpl

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
        val testOptionalUserEntity = Optional.of(testUserEntity)
        `when`(springDataUserRepository.findById(testUser.id!!)).thenReturn(testOptionalUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userDaoImpl.findById(testUser.id)

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
        val result = userDaoImpl.findById(notExistUserId)

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
        val result = userDaoImpl.findByEmail(TEST_EMAIL)

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
        val result = userDaoImpl.findByEmail(TEST_EMAIL)

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
        val result = userDaoImpl.findAll(pageable)

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
        val result = userDaoImpl.findActiveUserByEmail(TEST_EMAIL)

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
        val result = userDaoImpl.findActiveUserByEmail(TEST_EMAIL)

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
        val result = userDaoImpl.findSliceByEmailAndNickname("owner@owner.com", TEST_NICKNAME, pageable)

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
        val result = userDaoImpl.save(testUser)

        // then
        assertThat(result).isEqualTo(testUser)
        verify(springDataUserRepository).save(testUserEntity)
    }

    @Test
    @DisplayName("PENDING 상태의 사용자 ID를 주면 활성화(ACTIVE) 처리한다")
    fun activate_success() {
        // given
        val userId = UUID.randomUUID()
        val pendingUserEntity = UserJpaEntity(
            TEST_EMAIL,
            TEST_NICKNAME,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            status = UserStatus.PENDING
        )
        `when`(springDataUserRepository.findById(userId)).thenReturn(Optional.of(pendingUserEntity))

        // when
        userDaoImpl.activate(userId)

        // then
        assertThat(pendingUserEntity.status).isEqualTo(UserStatus.ACTIVE)
        verify(springDataUserRepository).save(pendingUserEntity)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 활성화 시도 시 예외가 발생한다")
    fun activate_fail_when_user_not_found() {
        // given
        val userId = UUID.randomUUID()
        `when`(springDataUserRepository.findById(userId)).thenReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            userDaoImpl.activate(userId)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("사용자의 닉네임을 변경하고 수정된 유저를 반환한다")
    fun updateNickname_success() {
        // given
        val newNickname = "새닉네임"
        `when`(springDataUserRepository.findByEmail(TEST_EMAIL)).thenReturn(testUserEntity)
        `when`(springDataUserRepository.save(testUserEntity)).thenReturn(testUserEntity)
        `when`(userMapper.toDomain(testUserEntity)).thenReturn(testUser)

        // when
        val result = userDaoImpl.updateNickname(TEST_EMAIL, newNickname)

        // then
        assertThat(result).isNotNull
        verify(springDataUserRepository).save(testUserEntity)
    }

    @Test
    @DisplayName("차단(block) 호출 시 사용자를 차단 상태로 만들고 저장한다")
    fun block_success() {
        // given
        `when`(springDataUserRepository.findByEmail(TEST_EMAIL)).thenReturn(testUserEntity)

        // when
        userDaoImpl.block(TEST_EMAIL)

        // then
        assertThat(testUserEntity.status).isEqualTo(UserStatus.BLOCKED)
        verify(springDataUserRepository).save(testUserEntity)
    }

    @Test
    @DisplayName("차단 해제(unblock) 호출 시 사용자를 활성 상태로 만들고 저장한다")
    fun unblock_success() {
        // given
        val blockedUserEntity = UserJpaEntity(
            TEST_EMAIL,
            TEST_NICKNAME,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            status = UserStatus.BLOCKED
        )
        `when`(springDataUserRepository.findByEmail(TEST_EMAIL)).thenReturn(blockedUserEntity)

        // when
        userDaoImpl.unblock(TEST_EMAIL)

        // then
        assertThat(blockedUserEntity.status).isEqualTo(UserStatus.ACTIVE)
        verify(springDataUserRepository).save(blockedUserEntity)
    }
}
