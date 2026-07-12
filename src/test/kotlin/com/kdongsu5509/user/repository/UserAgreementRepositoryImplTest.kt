package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermRepository
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserAgreementJpaEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserAgreementRepositoryImplTest {

    @Mock
    lateinit var userRepository: SpringDataUserRepository

    @Mock
    lateinit var termRepository: TermRepository

    @Mock
    lateinit var userAgreementRepository: SpringDataUserAgreementRepository

    @InjectMocks
    lateinit var userAgreementDaoImpl: UserAgreementRepositoryImpl

    @Test
    @DisplayName("단일 약관 동의 정보를 성공적으로 저장한다")
    fun save_success() {
        // given
        val userId = UUID.randomUUID()
        val termId = 1L
        val userEntity =
            UserJpaEntity("test@kakao.com", "테스트", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        userEntity.id = userId

        val term = Term.reconstruct(termId, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)

        given(userRepository.findById(userId)).willReturn(Optional.of(userEntity))
        given(termRepository.findById(termId)).willReturn(term)

        // when
        userAgreementDaoImpl.save(userId, termId)

        // then
        verify(userRepository).findById(userId)
        verify(termRepository).findById(termId)
        verify(userAgreementRepository).save(any<UserAgreementJpaEntity>())
    }

    @Test
    @DisplayName("전달받은 약관 ID에 해당하는 동의 정보만 일괄 저장한다")
    fun saveAll_success() {
        // given
        val userId = UUID.randomUUID()
        val userEntity =
            UserJpaEntity("test@kakao.com", "테스트", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        userEntity.id = userId

        val term1 = Term.reconstruct(1L, 1L, TermTypes.SERVICE, "서비스 약관", "내용1", LocalDateTime.now(), true)
        val term2 = Term.reconstruct(2L, 1L, TermTypes.PRIVACY, "정보 처리 약관", "내용2", LocalDateTime.now(), true)

        given(userRepository.findById(userId)).willReturn(Optional.of(userEntity))
        given(termRepository.findById(1L)).willReturn(term1)
        given(termRepository.findById(2L)).willReturn(term2)

        // when
        userAgreementDaoImpl.saveAll(userId, listOf(1L, 2L))

        // then: 전달받은 id로 조회하고, findActiveAll 전체 저장이 아닌 조회분만 저장한다
        verify(userRepository).findById(userId)
        verify(termRepository).findById(1L)
        verify(termRepository).findById(2L)
        verify(userAgreementRepository).saveAll(any<List<UserAgreementJpaEntity>>())
    }

    @Test
    @DisplayName("saveAll 호출 시 존재하지 않는 약관 ID가 들어오면 예외가 발생한다")
    fun saveAll_fail_when_term_not_found() {
        // given
        val userId = UUID.randomUUID()
        val userEntity =
            UserJpaEntity("test@kakao.com", "테스트", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        userEntity.id = userId

        given(userRepository.findById(userId)).willReturn(Optional.of(userEntity))
        given(termRepository.findById(9999L)).willReturn(null)

        // when & then
        assertThatThrownBy {
            userAgreementDaoImpl.saveAll(userId, listOf(9999L))
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(TermException.TERM_NOT_FOUND)
    }

    @Test
    @DisplayName("save 호출 시 존재하지 않는 사용자 ID가 들어오면 예외가 발생한다")
    fun save_fail_when_user_not_found() {
        // given
        val userId = UUID.randomUUID()
        given(userRepository.findById(userId)).willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            userAgreementDaoImpl.save(userId, 1L)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID로 저장 시도 시 예외가 발생한다")
    fun save_fail_when_term_not_found() {
        // given
        val userId = UUID.randomUUID()
        val termId = 9999L
        val userEntity =
            UserJpaEntity("test@kakao.com", "테스트", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        userEntity.id = userId

        given(userRepository.findById(userId)).willReturn(Optional.of(userEntity))
        given(termRepository.findById(termId)).willReturn(null)

        // when & then
        assertThatThrownBy {
            userAgreementDaoImpl.save(userId, termId)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(TermException.TERM_NOT_FOUND)
    }

    @Test
    @DisplayName("saveAll 호출 시 존재하지 않는 사용자 ID가 들어오면 예외가 발생한다")
    fun saveAll_fail_when_user_not_found() {
        // given
        val userId = UUID.randomUUID()
        given(userRepository.findById(userId)).willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            userAgreementDaoImpl.saveAll(userId, listOf(1L, 2L))
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }
}
