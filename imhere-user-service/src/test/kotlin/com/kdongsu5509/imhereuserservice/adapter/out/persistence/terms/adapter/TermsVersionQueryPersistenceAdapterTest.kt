package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TermsVersionQueryPersistenceAdapterTest {

    companion object {
        const val TERM_DEFINITION_ID = 100L
        const val TERM_DEFINITION_TITLE = "테스트 약관"
        val TERM_TYPE = TermsTypes.LOCATION
        const val VERSION = "v1.0"
        const val CONTENT = "내용"
        var effectiveDate: LocalDateTime = LocalDateTime.now()
    }

    val termVersionMapper = TermVersionMapper()

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
    @DisplayName("존재하는 ID 조회 시 도메인 엔티티를 반환한다")
    fun loadSpecificTermVersion_success() {
        // given
        val termsDefinitionJpaEntity = TermsDefinitionJpaEntity(
            TERM_DEFINITION_TITLE, TERM_TYPE, true
        )
        termsDefinitionJpaEntity.id = TERM_DEFINITION_ID
        val jpaEntity = TermsVersionJpaEntity(
            VERSION, CONTENT, true, effectiveDate, termsDefinitionJpaEntity
        )

        given(springDataTermsVersionRepository.findActiveVersion(TERM_DEFINITION_ID)).willReturn(Optional.of(jpaEntity))

        // when
        val result = adapter.loadSpecificActiveTermVersion(TERM_DEFINITION_ID)

        // then
        assertThat(result.content).isEqualTo(CONTENT)
        assertThat(result.version).isEqualTo(VERSION)
        assertThat(result.effectiveDate).isEqualTo(effectiveDate)
        assertThat(result.termDefinitionId).isEqualTo(TERM_DEFINITION_ID)
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID로 version 조회 요청 시 TERM_DEFINITION_NOT_FOUND 예외가 발생해야 한다")
    fun loadSpecificTermVersion_fail_not_found() {
        // given
        val invalidTermDefinitionId = 999L
        val termsDefinitionJpaEntity = TermsDefinitionJpaEntity(
            TERM_DEFINITION_TITLE, TERM_TYPE, true
        )
        termsDefinitionJpaEntity.id = invalidTermDefinitionId

        given(springDataTermsVersionRepository.findActiveVersion(invalidTermDefinitionId)).willReturn(Optional.empty())

        // when, then
        assertThatThrownBy {
            adapter.loadSpecificActiveTermVersion(invalidTermDefinitionId)
        }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TERM_DEFINITION_NOT_FOUND)
    }
}