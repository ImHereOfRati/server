package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

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
        // 테스트용 유저 저장
        testUser = userRepository.save(
            UserJpaEntity("test@kakao.com", "테스터", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        )

        val testTermDef1 =
            termsDefinitionRepository.save(TermsDefinitionJpaEntity(termsTitle = "테스트 약관1", TermsTypes.LOCATION, true))
        val testTermDef2 =
            termsDefinitionRepository.save(TermsDefinitionJpaEntity(termsTitle = "테스트 약관2", TermsTypes.PRIVACY, true))

        createTestTermVersionEntity(testTermDef1)
        createTestTermVersionEntity(testTermDef2)

        termDefId1 = testTermDef1.id!!
        termDefId2 = testTermDef2.id!!
    }

    private fun createTestTermVersionEntity(testTermDef: TermsDefinitionJpaEntity) {
        termsVersionRepository.save(
            TermsVersionJpaEntity(
                version = "1.0",
                content = "내용",
                isActive = true,
                effectiveDate = LocalDateTime.now().plusDays(1),
                testTermDef
            )
        )
    }

    @Test
    @DisplayName("단일 약관 동의 정보를 성공적으로 저장한다")
    fun save_single_agreement_success() {
        // when
        adapter.saveAgreement(testUser.email, termDefId1)

        // then
        val results = agreementRepository.findAll()
        assertThat(results).hasSize(1)
        assertThat(results[0].user.email).isEqualTo(testUser.email)
        assertThat(results[0].termsVersion.terms.id).isEqualTo(termDefId1)
    }

    @Test
    @DisplayName("여러 개의 약관 동의 정보를 한 번에 저장한다")
    fun save_multiple_agreements_success() {
        // given
        val termIds = listOf(termDefId1, termDefId2)

        // when
        adapter.saveAgreements(testUser.email, termIds)

        // then
        val results = agreementRepository.findAll()
        assertThat(results).hasSize(2)
        val savedTermVersionIds = results.map { it.termsVersion.terms.id }
        assertThat(savedTermVersionIds).containsExactlyInAnyOrder(termDefId1, termDefId2)
    }

    @Test
    @DisplayName("존재하지 않는 유저 이메일로 저장 시 USER_NOT_FOUND 예외가 발생한다")
    fun save_fail_user_not_found() {
        assertThatThrownBy {
            adapter.saveAgreement("wrong@kakao.com", termDefId1)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND)
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID가 포함된 경우 TERM_DEFINITION_NOT_FOUND 예외가 발생한다")
    fun save_fail_term_not_found() {
        val invalidTermIds = listOf(termDefId1, 9999L)

        assertThatThrownBy {
            adapter.saveAgreements(testUser.email, invalidTermIds)
        }.isInstanceOf(BusinessException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TERM_DEFINITION_NOT_FOUND)
    }
}