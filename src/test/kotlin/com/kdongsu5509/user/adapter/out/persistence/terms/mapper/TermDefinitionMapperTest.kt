package com.kdongsu5509.user.adapter.out.persistence.terms.mapper

import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.domain.terms.TermDefinition
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class TermDefinitionMapperTest {

    companion object {
        const val TERM_TITLE = "테스트용 약관"
        val testTermsType = TermsTypes.LOCATION
    }

    private val termDefinitionMapper: TermDefinitionMapper = TermDefinitionMapper()

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    @DisplayName("도메인 파라미터로 JPA 엔티티를 성공적으로 생성한다")
    fun mapToJpaEntity_success(testIsRequired: Boolean) {
        // when
        val result = termDefinitionMapper.mapToJpaEntity(TERM_TITLE, testTermsType, testIsRequired)

        // then
        assertThat(result).isInstanceOf(TermsDefinitionJpaEntity::class.java)
        assertThat(result.termsTitle).isEqualTo(TERM_TITLE)
        assertThat(result.termsType).isEqualTo(testTermsType)
        assertThat(result.isRequired).isEqualTo(testIsRequired)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    @DisplayName("JPA 엔티티를 도메인 모델로 성공적으로 변환한다")
    fun mapToDomainEntity_success(testIsRequired: Boolean) {
        // given
        val testJpaEntity = TermsDefinitionJpaEntity(
            TERM_TITLE, testTermsType, testIsRequired
        ).apply {
            id = 999L
        }

        // when
        val result = termDefinitionMapper.mapToDomainEntity(testJpaEntity)

        // then
        assertThat(result).isInstanceOf(TermDefinition::class.java)
        assertThat(result.title).isEqualTo(TERM_TITLE)
        assertThat(result.termsTypes).isEqualTo(testTermsType)
        assertThat(result.isRequired).isEqualTo(testIsRequired)
    }
}
