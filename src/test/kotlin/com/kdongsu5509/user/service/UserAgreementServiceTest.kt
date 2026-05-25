package com.kdongsu5509.user.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserAgreementRepository
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand.TermConsentCommand
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
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class UserAgreementServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var termService: TermService

    @Mock
    lateinit var userAgreementRepository: UserAgreementRepository

    @InjectMocks
    lateinit var userAgreementService: UserAgreementService

    companion object {
        const val TEST_EMAIL = "test@test.com"
        const val TEST_NICKNAME = "테스트"
        val userId = UUID.randomUUID()

        fun createPendingUser() = User(
            id = userId,
            email = TEST_EMAIL,
            nickname = TEST_NICKNAME,
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.PENDING
        )
    }

    @Test
    @DisplayName("필수 약관에 모두 동의하고 사용자를 활성화하면 동의 기록을 저장하고 활성화된 사용자를 반환한다")
    fun consentAll_success() {
        // given
        val termResult1 = TermResult(1L, 1L, TermTypes.SERVICE, "서비스 약관", "내용", LocalDateTime.now(), true)
        val termResult2 = TermResult(2L, 1L, TermTypes.PRIVACY, "개인정보 약관", "내용", LocalDateTime.now(), true)
        val termResult3 = TermResult(3L, 1L, TermTypes.MARKETING, "마케팅 약관", "내용", LocalDateTime.now(), false)

        val pendingUser = createPendingUser()
        `when`(termService.findAll(true)).thenReturn(listOf(termResult1, termResult2, termResult3))
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(pendingUser)

        val command = MultiTermsConsentCommand(
            consents = listOf(
                TermConsentCommand(1L, true),
                TermConsentCommand(2L, true),
                TermConsentCommand(3L, true)
            )
        )

        // when
        val result = userAgreementService.consentAll(TEST_EMAIL, command)

        // then
        assertThat(result.status).isEqualTo(UserStatus.ACTIVE)
        verify(userAgreementRepository).saveAll(userId, listOf(1L, 2L, 3L))
        verify(userRepository).update(any()) // activate(userId) 대신 인터페이스 스펙인 update(user) 검증
    }

    @Test
    @DisplayName("필수 약관 중 하나라도 동의하지 않으면 예외가 발생한다")
    fun consentAll_fail_obligatory_term_not_agreed() {
        // given
        val termResult1 = TermResult(1L, 1L, TermTypes.SERVICE, "서비스 약관", "내용", LocalDateTime.now(), true)
        val termResult2 = TermResult(2L, 1L, TermTypes.PRIVACY, "개인정보 약관", "내용", LocalDateTime.now(), true)

        `when`(termService.findAll(true)).thenReturn(listOf(termResult1, termResult2))

        val command = MultiTermsConsentCommand(
            consents = listOf(
                TermConsentCommand(1L, true),
                TermConsentCommand(2L, false)
            )
        )

        // when & then
        assertThatThrownBy {
            userAgreementService.consentAll(TEST_EMAIL, command)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(TermException.OBLIGATORY_TERM_NOT_AGREED)
    }

    @Test
    @DisplayName("consentAll 호출 시 사용자가 존재하지 않으면 예외가 발생한다")
    fun consentAll_fail_user_not_found() {
        // given
        val termResult1 = TermResult(1L, 1L, TermTypes.SERVICE, "서비스 약관", "내용", LocalDateTime.now(), true)
        `when`(termService.findAll(true)).thenReturn(listOf(termResult1))
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null)

        val command = MultiTermsConsentCommand(
            consents = listOf(
                TermConsentCommand(1L, true)
            )
        )

        // when & then
        assertThatThrownBy {
            userAgreementService.consentAll(TEST_EMAIL, command)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("개별 약관에 동의하면 정상적으로 동의 기록을 저장한다")
    fun consent_success() {
        // given
        val pendingUser = createPendingUser()
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(pendingUser)

        // when
        userAgreementService.consent(TEST_EMAIL, 1L)

        // then
        verify(userAgreementRepository).save(userId, 1L)
    }

    @Test
    @DisplayName("개별 약관 동의 시 사용자가 존재하지 않으면 예외가 발생한다")
    fun consent_fail_user_not_found() {
        // given
        `when`(userRepository.findByEmail(TEST_EMAIL)).thenReturn(null)

        // when & then
        assertThatThrownBy {
            userAgreementService.consent(TEST_EMAIL, 1L)
        }.isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(UserException.USER_NOT_FOUND)
    }
}
