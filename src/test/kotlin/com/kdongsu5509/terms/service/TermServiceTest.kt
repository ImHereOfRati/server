package com.kdongsu5509.terms.service

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.TermPersistenceAdapter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TermServiceTest {

    @Mock
    lateinit var termPersistenceAdapter: TermPersistenceAdapter

    @InjectMocks
    lateinit var termService: TermService

    @Test
    @DisplayName("새로운 약관을 저장할 때 기존 약관이 없으면 버전 1로 저장한다")
    fun save_new_term_without_existing() {
        // given
        val command = TermCreateCommand(
            type = TermTypes.SERVICE,
            title = "서비스 이용약관",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        given(termPersistenceAdapter.findLatestByType(command.type)).willReturn(null)
        given(termPersistenceAdapter.save(any())).willReturn(
            Term(1L, 1L, command.type, command.title, command.content, command.effectiveDate, command.isRequired)
        )

        // when
        val result = termService.save(command)

        // then
        assertThat(result.version).isEqualTo(1L)
        then(termPersistenceAdapter).should().findLatestByType(command.type)
        then(termPersistenceAdapter).should().save(any())
    }

    @Test
    @DisplayName("새로운 약관을 저장할 때 기존 약관이 있으면 버전을 증가시켜 저장한다")
    fun save_new_term_with_existing() {
        // given
        val command = TermCreateCommand(
            type = TermTypes.SERVICE,
            title = "서비스 이용약관 v2",
            content = "내용 v2",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        val existingTerm = Term(
            id = 1L,
            version = 1L,
            type = TermTypes.SERVICE,
            title = "서비스 이용약관 v1",
            content = "내용 v1",
            effectiveDate = LocalDateTime.now().minusDays(1),
            isRequired = true
        )
        given(termPersistenceAdapter.findLatestByType(command.type)).willReturn(existingTerm)
        given(termPersistenceAdapter.save(any())).willReturn(
            Term(2L, 2L, command.type, command.title, command.content, command.effectiveDate, command.isRequired)
        )

        // when
        val result = termService.save(command)

        // then
        assertThat(result.version).isEqualTo(2L)
        then(termPersistenceAdapter).should().findLatestByType(command.type)
        then(termPersistenceAdapter).should().save(any())
    }

    @Test
    @DisplayName("모든 약관을 조회한다")
    fun findAll_success() {
        // given
        val terms = listOf(
            Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        given(termPersistenceAdapter.findAll()).willReturn(terms)

        // when
        val results = termService.findAll()

        // then
        assertThat(results).hasSize(1)
        then(termPersistenceAdapter).should().findAll()
    }

    @Test
    @DisplayName("isActive가 true일 때 활성화된 약관만 조회한다")
    fun findAll_active_true() {
        // given
        val terms = listOf(
            Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        given(termPersistenceAdapter.findActiveAll()).willReturn(terms)

        // when
        val results = termService.findAll(true)

        // then
        assertThat(results).hasSize(1)
        then(termPersistenceAdapter).should().findActiveAll()
    }

    @Test
    @DisplayName("isActive가 false일 때 예외가 발생한다")
    fun findAll_active_false_throws_exception() {
        // when & then
        assertThrows<ImHereBaseException> {
            termService.findAll(false)
        }.also {
            assertThat(it.errorCode).isEqualTo(TermException.NON_ACTIVE_TERM_NOT_ALLOWED)
        }
    }
}
