package com.kdongsu5509.terms.repository

import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TermMapperTest {

    private val termMapper = TermMapper()

    @Test
    @DisplayName("엔티티를 도메인 객체로 변환한다")
    fun toDomain() {
        // given
        val entity = TermJpaEntity(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)

        // when
        val domain = termMapper.toDomain(entity)

        // then
        assertThat(domain).isNotNull
        assertThat(domain?.id).isEqualTo(1L)
        assertThat(domain?.version).isEqualTo(1L)
        assertThat(domain?.type).isEqualTo(TermTypes.SERVICE)
        assertThat(domain?.title).isEqualTo("제목")
        assertThat(domain?.content).isEqualTo("내용")
        assertThat(domain?.isRequired).isTrue()
    }

    @Test
    @DisplayName("도메인 객체를 엔티티로 변환한다")
    fun toEntity() {
        // given
        val domain = Term(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)

        // when
        val entity = termMapper.toEntity(domain)

        // then
        assertThat(entity.version).isEqualTo(1L)
        assertThat(entity.type).isEqualTo(TermTypes.SERVICE)
        assertThat(entity.title).isEqualTo("제목")
        assertThat(entity.content).isEqualTo("내용")
        assertThat(entity.isRequired).isTrue()
    }

    @Test
    @DisplayName("엔티티가 null이면 null을 반환한다")
    fun toDomain_null() {
        // when
        val domain = termMapper.toDomain(null)

        // then
        assertThat(domain).isNull()
    }
}
