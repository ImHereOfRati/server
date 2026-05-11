package com.kdongsu5509.user.adapter.out.persistence.terms.mapper

import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TermVersionMapperTest {

    companion object {
        const val TERM_TITLE = "?�스?�용 ?��?"
        val testTermType = TermsTypes.LOCATION
        const val VERSION = "v1.0"
        const val CONTENT = "?��? ?�용?�니??"
        val EFFECTIVE_DATE: LocalDateTime? = LocalDateTime.now()
    }

    private val termVersionMapper = TermVersionMapper()

    @Test
    @DisplayName("?�력 ?�이?�들??바탕?�로 TermsVersionJpaEntity�??�바르게 변?�해???�다")
    fun shouldMapToJpaEntity() {
        // given
        val termDefinition = TermsDefinitionJpaEntity(TERM_TITLE, testTermType, true)

        // when
        val jpaEntity = termVersionMapper.mapToJpaEntity(termDefinition, VERSION, CONTENT, EFFECTIVE_DATE!!)

        // then
        assertThat(jpaEntity.version).isEqualTo(VERSION)
        assertThat(jpaEntity.termVersionContent).isEqualTo(CONTENT)
        assertThat(jpaEntity.effectiveDate).isEqualTo(EFFECTIVE_DATE)
        assertThat(jpaEntity.terms).isEqualTo(termDefinition)
        assertThat(jpaEntity.isActive).isTrue() // 매퍼?�서 true�?고정 ?�정??    }

        @Test
        @DisplayName("TermsVersionJpaEntity�?Domain 객체??TermVersion?�로 ?�바르게 변?�해???�다")
        fun shouldMapToDomainEntity() {
            // given
            val termDefinition = TermsDefinitionJpaEntity(TERM_TITLE, testTermType, true)
            termDefinition.apply {
                id = 1L
            }
            val jpaEntity = TermsVersionJpaEntity(
                version = "v2.0",
                termVersionContent = "?�데?�트???�용",
                isActive = true,
                effectiveDate = LocalDateTime.of(2026, 2, 6, 0, 0),
                terms = termDefinition
            )

            // when
            val domainTermVersion = termVersionMapper.mapToDomainEntity(jpaEntity)

            // then
            assertThat(domainTermVersion.termDefinitionId).isEqualTo(jpaEntity.terms.id)
            assertThat(domainTermVersion.version).isEqualTo(jpaEntity.version)
            assertThat(domainTermVersion.content).isEqualTo(jpaEntity.termVersionContent)
            assertThat(domainTermVersion.effectiveDate).isEqualTo(jpaEntity.effectiveDate)
        }
    }
}
