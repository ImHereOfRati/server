package com.kdongsu5509.agreement.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.repository.jpa.AgreementJpaEntity
import com.kdongsu5509.agreement.repository.jpa.SpringDataAgreementRepository
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.domain.UserStatus.ACTIVE
import com.kdongsu5509.user.domain.UserStatus.PENDING
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermCreateCommand
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*

class AgreementControllerIntegrationTest : WebIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: SpringDataUserRepository

    @Autowired
    private lateinit var termService: TermService

    @Autowired
    private lateinit var agreementRepository: SpringDataAgreementRepository

    @Test
    @DisplayName("인증한 사용자의 약관 동의 이력만 조회한다")
    fun findHistory_success_returns_only_authenticated_users_agreement_history() {
        // given
        val savedUser = saveUser()
        val optionalTerm = saveTerm("Marketing terms", isRequired = false)
        agreementRepository.saveAll(
            listOf(
                AgreementJpaEntity(savedUser.id!!, optionalTerm.id, AgreementStatus.CONSENT),
                AgreementJpaEntity(UUID.randomUUID(), optionalTerm.id, AgreementStatus.WITHDRAW),
            )
        )

        // when & then
        mockMvc.perform(
            get("/api/agreements")
                .with(user(userDetails(savedUser.id!!)))
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].termId").value(optionalTerm.id))
            .andExpect(jsonPath("$.data[0].action").value("CONSENT"))
            .andExpect(jsonPath("$.data[0].occurredAt").exists())
    }

    @Test
    @DisplayName("가입 대기 사용자가 약관 동의 이력을 조회하면 접근을 거부한다")
    fun findHistory_fail_pending_user_cannot_access_agreement_history() {
        // given
        val pendingUser = saveUser(PENDING)

        // when & then
        mockMvc.perform(
            get("/api/agreements")
                .with(user(userDetails(pendingUser.id!!, PENDING)))
        ).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("가입 대기 사용자도 약관 동의를 등록할 수 있다")
    fun consent_success_allows_pending_user() {
        // given
        val pendingUser = saveUser(PENDING)
        val requiredTerm = saveTerm("Service terms")

        // when & then
        mockMvc.perform(
            post("/api/agreements")
                .with(csrf())
                .with(user(userDetails(pendingUser.id!!, PENDING)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentBody(requiredTerm.id, true))
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("동일한 약관 동의를 반복 요청해도 동의 이력을 한 번만 저장한다")
    fun consent_success_is_idempotent_for_same_request() {
        // given
        val savedUser = saveUser()
        val requiredTerm = saveTerm("Service terms v1")

        // when
        repeat(2) {
            mockMvc.perform(
                post("/api/agreements")
                    .with(csrf())
                    .with(user(userDetails(savedUser.id!!)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(consentBody(requiredTerm.id, true))
            ).andExpect(status().isNoContent)
        }

        // then
        val agreementHistory = agreementRepository.findHistoryByUserId(savedUser.id!!)
        assertThat(agreementHistory.map { it.termId }).containsExactly(requiredTerm.id)
    }

    @Test
    @DisplayName("약관 동의 철회 후 재동의하면 모든 변경 이력을 보존한다")
    fun consent_success_preserves_withdrawal_and_reconsent_history() {
        // given
        val savedUser = saveUser()
        val requiredTerm = saveTerm("Service terms v1")

        // when
        listOf(true, false, true).forEach { isAgreed ->
            mockMvc.perform(
                post("/api/agreements")
                    .with(csrf())
                    .with(user(userDetails(savedUser.id!!)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(consentBody(requiredTerm.id, isAgreed))
            ).andExpect(status().isNoContent)
        }

        // then
        assertThat(
            agreementRepository.findHistoryByUserId(savedUser.id!!)
                .map { it.action.name }
        ).containsExactly("CONSENT", "WITHDRAW", "CONSENT")
    }

    @Test
    @DisplayName("갱신 약관에 동의해도 이전 버전의 동의 이력을 보존한다")
    fun consentToRenewedTerm_success_preserves_previous_version_history() {
        // given
        val savedUser = saveUser()
        val previousTerm = saveTerm("Service terms v1")

        mockMvc.perform(
            post("/api/agreements")
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentBody(previousTerm.id, true))
        ).andExpect(status().isNoContent)

        val renewedTerm = saveTerm("Service terms v2")

        // when
        mockMvc.perform(
            post("/api/agreements/renewals/{termId}", renewedTerm.id)
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
        ).andExpect(status().isNoContent)

        // then
        val agreedTermIds = agreementRepository.findHistoryByUserId(savedUser.id!!)
            .map { it.termId }
        assertThat(agreedTermIds).containsExactlyInAnyOrder(previousTerm.id, renewedTerm.id)
    }

    @Test
    @DisplayName("이전 버전에 동의하지 않은 사용자가 갱신 약관에 동의하면 요청을 거부한다")
    fun consentToRenewedTerm_fail_previous_version_agreement_is_missing() {
        // given
        val savedUser = saveUser()
        saveTerm("Service terms v1")
        val renewedTerm = saveTerm("Service terms v2")

        // when & then
        mockMvc.perform(
            post("/api/agreements/renewals/{termId}", renewedTerm.id)
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
        ).andExpect(status().`is`(422))
            .andExpect(jsonPath("$.imhereResponseCode").value("AGREEMENT-701"))

        assertThat(agreementRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("가입 대기 사용자가 갱신 약관에 동의하면 접근을 거부한다")
    fun consentToRenewedTerm_fail_pending_user_cannot_consent() {
        // given
        val pendingUser = saveUser(PENDING)

        // when & then
        mockMvc.perform(
            post("/api/agreements/renewals/{termId}", 1L)
                .with(csrf())
                .with(user(userDetails(pendingUser.id!!, PENDING)))
        ).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("선택 약관 동의를 철회하면 동의와 철회 이력을 모두 보존한다")
    fun withdrawAgreement_success_preserves_consent_and_withdrawal_history() {
        // given
        val savedUser = saveUser()
        val optionalTerm = saveTerm("Marketing terms", isRequired = false)

        mockMvc.perform(
            post("/api/agreements")
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentBody(optionalTerm.id, true))
        ).andExpect(status().isNoContent)

        // when
        mockMvc.perform(
            delete("/api/agreements/{termId}", optionalTerm.id)
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
        ).andExpect(status().isNoContent)

        // then
        assertThat(
            agreementRepository.findHistoryByUserId(savedUser.id!!)
                .map { it.termId to it.action }
        ).containsExactly(
            optionalTerm.id to AgreementStatus.CONSENT,
            optionalTerm.id to AgreementStatus.WITHDRAW,
        )
    }

    @Test
    @DisplayName("가입 대기 사용자가 약관 동의를 철회하면 접근을 거부한다")
    fun withdrawAgreement_fail_pending_user_cannot_withdraw() {
        // given
        val pendingUser = saveUser(PENDING)

        // when & then
        mockMvc.perform(
            delete("/api/agreements/{termId}", 1L)
                .with(csrf())
                .with(user(userDetails(pendingUser.id!!, PENDING)))
        ).andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("필수 약관 동의를 철회하면 요청을 거부하고 기존 동의 이력을 유지한다")
    fun withdrawAgreement_fail_required_agreement_cannot_be_withdrawn() {
        // given
        val savedUser = saveUser()
        val requiredTerm = saveTerm("Service terms")

        mockMvc.perform(
            post("/api/agreements")
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(consentBody(requiredTerm.id, true))
        ).andExpect(status().isNoContent)

        // when & then
        mockMvc.perform(
            delete("/api/agreements/{termId}", requiredTerm.id)
                .with(csrf())
                .with(user(userDetails(savedUser.id!!)))
        ).andExpect(status().`is`(422))
            .andExpect(jsonPath("$.imhereResponseCode").value("AGREEMENT-700"))

        assertThat(
            agreementRepository.findHistoryByUserId(savedUser.id!!)
                .map { it.termId to it.action }
        ).containsExactly(
            requiredTerm.id to AgreementStatus.CONSENT,
        )
    }

    private fun saveUser(status: UserStatus = ACTIVE): UserJpaEntity = userRepository.save(
        UserJpaEntity(
            email = TEST_EMAIL,
            nickname = "tester",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = status,
        )
    )

    private fun saveTerm(title: String, isRequired: Boolean = true) = termService.save(
        TermCreateCommand(
            type = TermTypes.SERVICE,
            title = title,
            content = "content",
            effectiveDate = LocalDateTime.now().minusMinutes(1),
            isRequired = isRequired,
        )
    )

    private fun userDetails(userId: UUID, status: UserStatus = ACTIVE) = ImHereUserDetails(
        email = TEST_EMAIL,
        nickname = "tester",
        role = UserRole.NORMAL.name,
        status = status.name,
        userId = userId,
    )

    private fun consentBody(termId: Long, isAgreed: Boolean) =
        """{"consents":[{"id":$termId,"agreed":$isAgreed}]}"""

    companion object {
        const val TEST_EMAIL = "agreement@example.com"
    }
}
