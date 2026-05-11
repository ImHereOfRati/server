package com.kdongsu5509.user.adapter.out.persistence.terms.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapper
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapperTest.Companion.TERM_TITLE
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapperTest.Companion.testTermsType
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.*

@ExtendWith(MockitoExtension::class)
class TermsDefinitionQueryPersistenceAdapterTest {

    @Mock
    lateinit var springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository

    private val termDefinitionMapper = TermDefinitionMapper()

    private lateinit var adapter: TermsDefinitionQueryPersistenceAdapter

    @BeforeEach
    fun setUp() {
        adapter = TermsDefinitionQueryPersistenceAdapter(
            termDefinitionMapper,
            springDataTermsDefinitionRepository
        )
    }

    @Test
    @DisplayName("약관 제목과 타입이 일치하는 데이터가 있으면 true를 반환한다")
    fun checkExistence_success() {
        // given
        val title = "서비스 이용약관"
        val type = TermsTypes.SERVICE
        given(springDataTermsDefinitionRepository.existsByTermsTitleAndTermsType(title, type))
            .willReturn(true)

        // when
        val result = adapter.checkExistence(title, type)

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("일치하는 약관 제목이 없으면 false를 반환한다")
    fun checkExistence_fail_when_not_exist() {
        // given
        val title = "신규 약관"
        val type = TermsTypes.SERVICE
        given(springDataTermsDefinitionRepository.existsByTermsTitleAndTermsType(title, type))
            .willReturn(false)

        // when
        val result = adapter.checkExistence(title, type)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("ID로 약관 정의를 성공적으로 조회한다")
    fun loadTermDefinition_success() {
        // given
        val id = 1L
        val testJpaEntity = TermsDefinitionJpaEntity(
            TERM_TITLE, testTermsType, true
        ).apply { this.id = id }
        given(springDataTermsDefinitionRepository.findById(id)).willReturn(Optional.of(testJpaEntity))

        // when
        val result = adapter.loadTermDefinition(id)

        // then
        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo(TERM_TITLE)
        assertThat(result.termsTypes).isEqualTo(testTermsType)
        assertThat(result.isRequired).isTrue()
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 예외가 발생한다")
    fun loadTermDefinition_fail_when_not_exist() {
        // given
        val id = 1L
        given(springDataTermsDefinitionRepository.findById(id)).willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            adapter.loadTermDefinition(id)
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("약관 목록을 페이징하여 성공적으로 조회한다")
    fun loadAllTermsDefinitions_success() {
        // given
        val pageable = PageRequest.of(0, 10)
        val testDefinitionOne = TermsDefinitionJpaEntity("약관1", TermsTypes.SERVICE, true).apply { id = 1L }
        val testDefinitionTwo = TermsDefinitionJpaEntity("약관2", TermsTypes.PRIVACY, true).apply { id = 2L }
        val jpaEntities = listOf(testDefinitionOne, testDefinitionTwo)
        val page = PageImpl(jpaEntities, pageable, jpaEntities.size.toLong())

        given(springDataTermsDefinitionRepository.findAll(pageable)).willReturn(page)

        // when
        val result = adapter.loadAllTermsDefinitions(pageable)

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].title).isEqualTo("약관1")
        verify(springDataTermsDefinitionRepository).findAll(pageable)
    }

    @Test
    @DisplayName("약관 목록이 없으면 빈 페이징 결과를 반환한다")
    fun loadAllTermsDefinitions_success_when_empty() {
        // given
        val pageable = PageRequest.of(0, 10)
        given(springDataTermsDefinitionRepository.findAll(pageable))
            .willReturn(Page.empty(pageable))

        // when
        val result = adapter.loadAllTermsDefinitions(pageable)

        // then
        assertThat(result).isNotNull
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0L)
    }
}
