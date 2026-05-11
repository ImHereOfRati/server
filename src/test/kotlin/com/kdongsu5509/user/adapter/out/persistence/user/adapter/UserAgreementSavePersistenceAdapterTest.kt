package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.terms.TermsTypes
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@ActiveProfiles("test")
@DataJpaTest
@Import(UserAgreementSavePersistenceAdapter::class)
class UserAgreementSavePersistenceAdapterTest @Autowired constructor(
    private val adapter: UserAgreementSavePersistenceAdapter,
    private val userRepository: SpringDataUserRepository,
    private val termsDefinitionRepository: SpringDataTermsDefinitionRepository,
    private val termsVersionRepository: SpringDataTermsVersionRepository,
    private val agreementRepository: SpringDataUserAgreementRepository
) {

    private lateinit var testUser: UserJpaEntity
    private var termDefId1: Long = 0
    private var termDefId2: Long = 0

    @BeforeEach
    fun setUp() {
        testUser = userRepository.save(
            UserJpaEntity("test@kakao.com", "테스트", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        )

        val testTermDef1 =
            termsDefinitionRepository.save(
                TermsDefinitionJpaEntity(
                    termsTitle = "테스트 약관1",
                    TermsTypes.LOCATION,
                    true
                )
            )
        val testTermDef2 =
            termsDefinitionRepository.save(
                TermsDefinitionJpaEntity(
                    termsTitle = "테스트 약관2",
                    TermsTypes.PRIVACY,
                    true
                )
            )

        createTestTermVersionEntity(testTermDef1)
        createTestTermVersionEntity(testTermDef2)

        termDefId1 = testTermDef1.id!!
        termDefId2 = testTermDef2.id!!
    }

    private fun createTestTermVersionEntity(testTermDef: TermsDefinitionJpaEntity) {
        termsVersionRepository.save(
            TermsVersionJpaEntity(
                version = "1.0",
                termVersionContent = "내용",
                isActive = true,
                effectiveDate = LocalDateTime.now().plusDays(1),
                testTermDef
            )
        )
    }

    @Test
    @DisplayName("단일 약관 동의 정보를 성공적으로 저장한다")
    fun save_success() {
        // when
        adapter.save(testUser.email, termDefId1)

        // then
        val results = agreementRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].user.email).isEqualTo(testUser.email)
        assertThat(results[0].termsVersion.terms.id).isEqualTo(termDefId1)
    }

    @Test
    @DisplayName("여러 개의 약관 동의 정보를 한 번에 저장한다")
    fun saveAll_success() {
        // given
        val termIds = listOf(termDefId1, termDefId2)

        // when
        adapter.saveAll(testUser.email, termIds)

        // then
        val results = agreementRepository.findAll()
        assertThat(results).hasSize(2)
        val savedTermVersionIds = results.map { it.termsVersion.terms.id }
        assertThat(savedTermVersionIds).containsExactlyInAnyOrder(termDefId1, termDefId2)
    }

    @Test
    @DisplayName("존재하지 않는 사용자 이메일로 저장 시도 시 예외가 발생한다")
    fun save_fail_when_user_not_found() {
        // when & then
        assertThatThrownBy {
            adapter.save("wrong@kakao.com", termDefId1)
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID가 포함된 경우 예외가 발생한다")
    fun saveAll_fail_when_term_not_found() {
        // given
        val invalidTermIds = listOf(termDefId1, 9999L)

        // when & then
        assertThatThrownBy {
            adapter.saveAll(testUser.email, invalidTermIds)
        }.isInstanceOf(BaseException::class.java)
            .extracting("errorCategory")
            .isEqualTo(ErrorReason.NOT_FOUND)
    }
}
