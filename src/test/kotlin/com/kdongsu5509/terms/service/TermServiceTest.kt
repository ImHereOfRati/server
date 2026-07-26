package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import com.kdongsu5509.terms.repository.TermJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
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
    lateinit var termRepository: SpringDataTermRepository

    @Captor
    lateinit var termCaptor: ArgumentCaptor<TermJpaEntity>

    @InjectMocks
    lateinit var termService: TermService

    @Test
    @DisplayName("새로운 약관을 저장할 때 기존 약관이 없으면 버전 1로 저장한다")
    fun save_new_term_without_existing() {
        val command = command()
        given(termRepository.findTopByTypeOrderByVersionDesc(command.type)).willReturn(null)
        given(termRepository.save(any<TermJpaEntity>())).willReturn(entity(id = 1L, version = 1L))

        val result = termService.save(command)

        then(termRepository).should().save(termCaptor.capture())
        assertThat(termCaptor.value.version).isEqualTo(1L)
        assertThat(result.version).isEqualTo(1L)
    }

    @Test
    @DisplayName("새로운 약관을 저장할 때 기존 약관이 있으면 버전을 증가시켜 저장한다")
    fun save_new_term_with_existing() {
        val command = command()
        given(termRepository.findTopByTypeOrderByVersionDesc(command.type))
            .willReturn(entity(id = 1L, version = 2L))
        given(termRepository.save(any<TermJpaEntity>())).willReturn(entity(id = 2L, version = 3L))

        val result = termService.save(command)

        then(termRepository).should().save(termCaptor.capture())
        assertThat(termCaptor.value.version).isEqualTo(3L)
        assertThat(result.version).isEqualTo(3L)
    }

    @Test
    @DisplayName("모든 약관을 조회한다")
    fun findAll_success() {
        given(termRepository.findAll()).willReturn(listOf(entity(id = 1L)))

        val results = termService.findAll()

        assertThat(results).hasSize(1)
        then(termRepository).should().findAll()
    }

    @Test
    @DisplayName("시행된 약관 중 타입별 최고 버전을 조회한다")
    fun findEffectiveTerms_success() {
        val candidates = listOf(
            entity(id = 1L, version = 1L, type = TermTypes.SERVICE),
            entity(id = 2L, version = 2L, type = TermTypes.SERVICE),
            entity(id = 3L, version = 1L, type = TermTypes.PRIVACY),
        )
        given(termRepository.findAllByEffectiveDateLessThanEqual(any())).willReturn(candidates)

        val results = termService.findEffectiveTerms()

        assertThat(results).hasSize(2)
        assertThat(results.single { it.type == TermTypes.SERVICE }.version).isEqualTo(2L)
        assertThat(results.single { it.type == TermTypes.PRIVACY }.version).isEqualTo(1L)
    }

    @Test
    @DisplayName("아이디 목록으로 약관을 조회한다")
    fun findAllByIds_success() {
        val ids = setOf(1L, 3L)
        given(termRepository.findAllById(ids)).willReturn(
            listOf(
                entity(id = 1L, type = TermTypes.SERVICE),
                entity(id = 3L, type = TermTypes.PRIVACY),
            )
        )

        val results = termService.findAllByIds(ids)

        assertThat(results).extracting<Long> { it.id }.containsExactly(1L, 3L)
    }

    @Test
    @DisplayName("시행 중인 약관을 TermFact로 변환해 공개한다")
    fun findEffectiveTermFacts_success() {
        val candidates = listOf(
            entity(id = 1L, version = 1L, type = TermTypes.SERVICE),
            entity(id = 2L, version = 2L, type = TermTypes.SERVICE),
            entity(id = 3L, version = 1L, type = TermTypes.PRIVACY, isRequired = false),
        )
        given(termRepository.findAllByEffectiveDateLessThanEqual(any())).willReturn(candidates)

        val facts = termService.findEffectiveTermFacts()

        assertThat(facts).hasSize(2)
        val service = facts.single { it.type == TermTypes.SERVICE.name }
        assertThat(service.id).isEqualTo(2L)
        assertThat(service.version).isEqualTo(2L)
        assertThat(service.isRequired).isTrue()
        val privacy = facts.single { it.type == TermTypes.PRIVACY.name }
        assertThat(privacy.isRequired).isFalse()
    }

    @Test
    @DisplayName("아이디 목록으로 조회한 약관을 TermFact로 변환해 공개한다")
    fun findTermFacts_success() {
        val ids = setOf(1L, 3L)
        given(termRepository.findAllById(ids)).willReturn(
            listOf(
                entity(id = 1L, version = 4L, type = TermTypes.SERVICE),
                entity(id = 3L, version = 1L, type = TermTypes.PRIVACY, isRequired = false),
            )
        )

        val facts = termService.findTermFacts(ids)

        assertThat(facts).extracting<Long> { it.id }.containsExactly(1L, 3L)
        assertThat(facts.single { it.id == 1L }.type).isEqualTo(TermTypes.SERVICE.name)
        assertThat(facts.single { it.id == 1L }.version).isEqualTo(4L)
        assertThat(facts.single { it.id == 3L }.isRequired).isFalse()
    }

    @Test
    @DisplayName("조회된 약관이 없으면 빈 TermFact 목록을 반환한다")
    fun findTermFacts_success_when_empty() {
        val ids = setOf(99L)
        given(termRepository.findAllById(ids)).willReturn(emptyList())

        assertThat(termService.findTermFacts(ids)).isEmpty()
    }

    private fun command() = TermCreateCommand(
        type = TermTypes.SERVICE,
        title = "서비스 이용약관",
        content = "내용",
        effectiveDate = LocalDateTime.now(),
        isRequired = true,
    )

    private fun entity(
        id: Long,
        version: Long = 1L,
        type: TermTypes = TermTypes.SERVICE,
        isRequired: Boolean = true,
    ) = TermJpaEntity(
        id = id,
        version = version,
        type = type,
        title = "제목",
        content = "내용",
        effectiveDate = LocalDateTime.now().minusDays(1),
        isRequired = isRequired,
    )
}
