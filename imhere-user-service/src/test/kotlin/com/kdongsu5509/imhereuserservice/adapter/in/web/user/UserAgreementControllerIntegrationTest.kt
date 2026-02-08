package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest.ConsentDetail
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.AgreementTermUseCase
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@ActiveProfiles("test")
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class UserAgreementControllerIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var termsDefinitionRepository: SpringDataTermsDefinitionRepository

    @Autowired
    lateinit var termsVersionRepository: SpringDataTermsVersionRepository

    @Mock
    lateinit var agreementTermUseCase: AgreementTermUseCase

    companion object {
        const val BASE_URL = "/api/v1/user/terms"
        const val TEST_USER = "test@kakao.com"
    }

    @BeforeEach
    fun setUp() {
        userRepository.save(
            UserJpaEntity(TEST_USER, "테스터", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        )

        val testTermDef1 =
            termsDefinitionRepository.save(TermsDefinitionJpaEntity(termsTitle = "테스트 약관1", TermsTypes.LOCATION, true))
        val testTermDef2 =
            termsDefinitionRepository.save(TermsDefinitionJpaEntity(termsTitle = "테스트 약관2", TermsTypes.PRIVACY, true))

        createTestTermVersionEntity(testTermDef1)
        createTestTermVersionEntity(testTermDef2)
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
    @WithMockUser(username = TEST_USER)
    @DisplayName("전체 약관 동의 API가 정상적으로 호출된다")
    fun consentAll_success() {
        val request = UserTermsConsentRequest(
            consents = listOf(ConsentDetail(1L, true), ConsentDetail(2L, true))
        )

        mockMvc.perform(
            post("$BASE_URL/consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("단일 약관 동의 시 존재하지 않는 ID 전달하면 400 에러가 발생한다")
    fun consentSingle_fail_validation() {
        val invalidId = 99999L

        mockMvc.perform(
            post("$BASE_URL/consent/{termDefinitionId}", invalidId)
        )
            .andExpect(status().isNotFound)
    }
}