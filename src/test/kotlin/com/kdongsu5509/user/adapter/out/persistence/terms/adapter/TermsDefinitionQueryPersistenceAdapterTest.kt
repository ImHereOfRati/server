package com.kdongsu5509.user.adapter.out.persistence.terms.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.ErrorCode
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapper
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapperTest.Companion.TERM_TITLE
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapperTest.Companion.testTermsType
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
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
    @DisplayName("약관이 존재하는 경우에는 true을 반환한다.")
    fun checkExistence() {
        //given
        val title = "이용약관"
        val type = TermsTypes.SERVICE
        `when`(springDataTermsDefinitionRepository.existsByTermsTitleAndTermsType(title, type))
            .thenReturn(true)

        // when
        val result = adapter.checkExistence(title, type)

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("약관이 존재하지 않으면 false를 반환한다.")
    fun checkNotExistence() {
        //given
        val title = "신규 이용 약관"
        val type = TermsTypes.SERVICE
        `when`(springDataTermsDefinitionRepository.existsByTermsTitleAndTermsType(title, type))
            .thenReturn(false)

        // when
        val result = adapter.checkExistence(title, type)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("약관 상세 조회 시 데이터가 있으면 잘 반환한다.")
    fun loadTermDefinition() {
        // given
        val id = 1L
        val testJpaEntity = TermsDefinitionJpaEntity(
            TERM_TITLE, testTermsType, true
        )
        testJpaEntity.id = 10L
        `when`(springDataTermsDefinitionRepository.findById(id)).thenReturn(Optional.of(testJpaEntity))

        // when
        val result = adapter.loadTermDefinition(id)

        assertThat(result).isNotNull
        assertThat(result.title).isEqualTo(TERM_TITLE)
        assertThat(result.termsTypes).isEqualTo(testTermsType)
        assertThat(result.isRequired).isTrue()
    }

    @Test
    @DisplayName("약관 상세 조회 시 데이터가 없으면 예외가 발생한다")
    fun loadTermDefinition_notExist() {
        // given
        val id = 1L
        `when`(springDataTermsDefinitionRepository.findById(id)).thenReturn(Optional.empty())

        // when & then
        assertThrows<BusinessException> {
            adapter.loadTermDefinition(id)
        }.also {
            assertThat(it.errorCode).isEqualTo(ErrorCode.TERM_DEFINITION_NOT_FOUND)
        }
    }

    @Test
    @DisplayName("약관 목록 조회 시 페이징된 결과를 반환한다")
    fun loadAllTermsDefinitions_success() {
        // given
        val pageable = PageRequest.of(0, 10)
        val testDefinitionOne = TermsDefinitionJpaEntity("약관1", TermsTypes.SERVICE, true)
        val testDefinitionTwo = TermsDefinitionJpaEntity("약관2", TermsTypes.PRIVACY, true)
        testDefinitionOne.id = 1L
        testDefinitionTwo.id = 2L
        val jpaEntities = listOf(
            testDefinitionOne, testDefinitionTwo
        )
        val page = PageImpl(jpaEntities, pageable, jpaEntities.size.toLong())

        `when`(springDataTermsDefinitionRepository.findAll(pageable)).thenReturn(page)

        // when
        val result = adapter.loadAllTermsDefinitions(pageable)

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].title).isEqualTo("약관1")
        verify(springDataTermsDefinitionRepository).findAll(pageable)
    }

    @Test
    @DisplayName("약관 목록 조회 시 데이터가 없으면 빈 페이지를 반환한다")
    fun loadAllTermsDefinitions_success_if_not_exist() {
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