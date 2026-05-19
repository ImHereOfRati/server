package com.kdongsu5509.terms.adapter.out.jpa

import com.kdongsu5509.terms.domain.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class SpringDataTermContentRepositoryTest @Autowired constructor(
    private val termContentRepository: SpringDataTermContentRepository,
    private val termsPolicyRepository: SpringDataTermPolicyRepository
) {

    @Test
    @DisplayName("특정 약관의 활성화된(isActive=true) 버전을 아이디 기반으로 조회한다")
    fun findCurrentByPolicyId_success() {
        // given
        val testTermPolicy = TermPolicyJpaEntity("서비스 이용약관", TermsTypes.SERVICE, true)
        val policy = termsPolicyRepository.save(testTermPolicy)
        val testTermContent = TermContentJpaEntity("1", "내용", LocalDateTime.now(), true, policy)
        val testTermContent2 = TermContentJpaEntity("1", "내용", LocalDateTime.now(), false, policy)

        termContentRepository.save(testTermContent)
        termContentRepository.save(testTermContent2)

        // when
        val result = termContentRepository.findCurrentByPolicyId(policy.id!!)

        // then
        assertThat(result).isNotNull
        assertThat(result?.version).isEqualTo("1")
        assertThat(result?.isActive).isTrue()
    }

    @Test
    @DisplayName("활성화된 버전이 없으면 null을 반환한다")
    fun findCurrentByPolicyId_fail() {
        // given
        val testTermPolicy = TermPolicyJpaEntity("서비스 이용약관", TermsTypes.SERVICE, true)
        val policy = termsPolicyRepository.save(testTermPolicy)
        val testTermContent = TermContentJpaEntity("1", "내용", LocalDateTime.now(), false, policy)

        // when
        val result = termContentRepository.findCurrentByPolicyId(policy.id!!)

        // then
        assertThat(result).isNull()
    }
}
