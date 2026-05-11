package com.kdongsu5509.user.adapter.out.persistence.terms.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TermsVersionQueryPersistenceAdapterTest {

    companion object {
        const val TERM_DEFINITION_ID = 100L
        const val TERM_DEFINITION_TITLE = "테스트 약관"
        val TERM_TYPE = TermsTypes.LOCATION
        const val VERSION = "v1.0"
        const val CONTENT = "약관 내용"
        val effectiveDate: LocalDateTime = LocalDateTime.now()
    }

    private val termVersionMapper = TermVersionMapper()

    @Mock
    lateinit var springDataTermsVersionRepository: SpringDataTermsVersionRepository

    private lateinit var adapter: TermsVersionQueryPersistenceAdapter

    @BeforeEach
    fun setUp() {
        adapter = TermsVersionQueryPersistenceAdapter(
            termVersionMapper,
            springDataTermsVersionRepository
        )
    }

    @Test
    @DisplayName("활성화된 약관 버전이 존재하면 성공적으로 반환한다")
    fun loadSpecificActiveTermVersion_success() {
        // given
        val termsDefinitionJpaEntity = TermsDefinitionJpaEntity(
            TERM_DEFINITION_TITLE, TERM_TYPE, true
        ).apply { id = TERM_DEFINITION_ID }

        val jpaEntity = TermsVersionJpaEntity(
            VERSION, CONTENT, true, effectiveDate, termsDefinitionJpaEntity
        )

        given(springDataTermsVersionRepository.findActiveVersion(TERM_DEFINITION_ID)).willReturn(jpaEntity)

        // when
        val result = adapter.loadSpecificActiveTermVersion(TERM_DEFINITION_ID)

        // then
        assertThat(result.content).isEqualTo(CONTENT)
        assertThat(result.version).isEqualTo(VERSION)
        assertThat(result.effectiveDate).isEqualTo(effectiveDate)
        assertThat(result.termDefinitionId).isEqualTo(TERM_DEFINITION_ID)
    }

    @Test
    @DisplayName("활성화된 약관 버전이 존재하지 않으면 예외가 발생한다")
    fun loadSpecificActiveTermVersion_fail_when_not_found() {
        // given
        val invalidTermDefinitionId = 999L
        given(springDataTermsVersionRepository.findActiveVersion(invalidTermDefinitionId)).willReturn(null)

        // when & then
        assertThatThrownBy {
            adapter.loadSpecificActiveTermVersion(invalidTermDefinitionId)
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }
}
