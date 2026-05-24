package com.kdongsu5509.terms.adapter.out

import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TermPersistenceAdapterTest {

    @Mock
    lateinit var termMapper: TermMapper

    @Mock
    lateinit var termRepository: SpringDataTermRepository

    @InjectMocks
    lateinit var termPersistenceAdapter: TermPersistenceAdapter

    @Test
    @DisplayName("약관을 성공적으로 저장한다")
    fun save_success() {
        // given
        val term = Term(null, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        val entity = TermJpaEntity(null, 1L, TermTypes.SERVICE, "제목", "내용", term.effectiveDate, true)
        val savedEntity = TermJpaEntity(1L, 1L, TermTypes.SERVICE, "제목", "내용", term.effectiveDate, true)
        val savedTerm = Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", term.effectiveDate, true)

        given(termMapper.toEntity(term)).willReturn(entity)
        given(termRepository.save(entity)).willReturn(savedEntity)
        given(termMapper.toDomain(savedEntity)).willReturn(savedTerm)

        // when
        val result = termPersistenceAdapter.save(term)

        // then
        assertThat(result.id).isEqualTo(1L)
        verify(termRepository).save(entity)
    }

    @Test
    @DisplayName("타입으로 약관을 조회한다")
    fun findLatestByType() {
        // given
        val type = TermTypes.SERVICE
        val entity = TermJpaEntity(1L, 1L, type, "제목", "내용", LocalDateTime.now(), true)
        val term = Term(1L, 1L, type, "제목", "내용", entity.effectiveDate, true)

        given(termRepository.findLatestByType(type)).willReturn(entity)
        given(termMapper.toDomain(entity)).willReturn(term)

        // when
        val result = termPersistenceAdapter.findLatestByType(type)

        // then
        assertThat(result).isNotNull
        assertThat(result?.type).isEqualTo(type)
    }

    @Test
    @DisplayName("모든 약관을 조회한다")
    fun findAll() {
        // given
        val entity = TermJpaEntity(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        val term = Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", entity.effectiveDate, true)

        given(termRepository.findAll()).willReturn(listOf(entity))
        given(termMapper.toDomain(entity)).willReturn(term)

        // when
        val results = termPersistenceAdapter.findAll()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo(1L)
    }

    @Test
    @DisplayName("활성화된 모든 약관을 조회한다")
    fun findActiveAll() {
        // given
        val entity = TermJpaEntity(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        val term = Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", entity.effectiveDate, true)

        given(termRepository.findActiveAll()).willReturn(listOf(entity))
        given(termMapper.toDomain(entity)).willReturn(term)

        // when
        val results = termPersistenceAdapter.findActiveAll()

        // then
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo(1L)
    }
}
