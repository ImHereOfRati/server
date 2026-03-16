package com.kdongsu5509.user.adapter.out.persistence.terms.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class TermsVersionCommandPersistenceAdapterTest {

    var termVersionMapper: TermVersionMapper = TermVersionMapper()

    @Mock
    lateinit var springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository

    @Mock
    lateinit var springDataTermsVersionRepository: SpringDataTermsVersionRepository

    private lateinit var adapter: TermsVersionCommandPersistenceAdapter

    @BeforeEach
    fun setUp() {
        adapter = TermsVersionCommandPersistenceAdapter(
            termVersionMapper,
            springDataTermsDefinitionRepository,
            springDataTermsVersionRepository
        )
    }

    @Test
    @DisplayName("정상적인 경우에는 잘 저장한다")
    fun saveTermVersion_success() {
        // given
        val validTermDefinitionId = 100L
        val testTermDefinitionTitle = "테스트 약관"
        val testTermDefinitionType = TermsTypes.LOCATION
        val version = "v1.0"
        val content = "내용"
        val effectiveDate = LocalDateTime.now()

        val termsDefinitionJpaEntity = TermsDefinitionJpaEntity(
            testTermDefinitionTitle, testTermDefinitionType, true
        )

        given(springDataTermsDefinitionRepository.findById(validTermDefinitionId))
            .willReturn(Optional.of(termsDefinitionJpaEntity))
        given(springDataTermsVersionRepository.findActiveVersion(validTermDefinitionId))
            .willReturn(Optional.empty())

        // when & then
        assertDoesNotThrow {
            adapter.saveTermVersion(validTermDefinitionId, version, content, effectiveDate)
        }

        verify(springDataTermsDefinitionRepository).findById(validTermDefinitionId)
        verify(springDataTermsVersionRepository).save(any())
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID로 저장 요청 시 TERM_DEFINITION_NOT_FOUND 예외가 발생해야 한다")
    fun saveTermVersion_fail_definitionNotFound() {
        // given
        val invalidId = 999L
        val version = "v1.0"
        val content = "내용"
        val effectiveDate = LocalDateTime.now()

        given(springDataTermsDefinitionRepository.findById(invalidId))
            .willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            adapter.saveTermVersion(invalidId, version, content, effectiveDate)
        }
            .isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TERM_DEFINITION_NOT_FOUND)
    }
}