package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.adapter.`in`.web.dto.UserActivationRequest
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermCreateCommand
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class UserActivationControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProviderPort: ImHereTokenProviderPort

    @Autowired
    private lateinit var termService: TermService

    @Test
    @DisplayName("PENDING user can activate successfully")
    fun activationSuccessAndDocument() {
        val email = "pending@example.com"
        val user = User.createWithPendingStatus(email, "Pending User", OAuth2Provider.KAKAO)
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val initialToken = tokenProviderPort.issue(claims)

        val requiredTerm = termService.save(
            TermCreateCommand(
                type = TermTypes.SERVICE,
                title = "Required Term",
                content = "Content",
                effectiveDate = LocalDateTime.now().minusDays(1),
                isRequired = true
            )
        )

        val optionalTerm = termService.save(
            TermCreateCommand(
                type = TermTypes.MARKETING,
                title = "Optional Term",
                content = "Content",
                effectiveDate = LocalDateTime.now().minusDays(1),
                isRequired = false
            )
        )

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = requiredTerm.id, isAgreed = true),
                UserActivationRequest.TermConsent(id = optionalTerm.id, isAgreed = false)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${initialToken.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("consents").description("Terms consent list"),
                            fieldWithPath("consents[].id").description("Terms ID"),
                            fieldWithPath("consents[].agreed").description("Whether consent was given")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("Response code"),
                            fieldWithPath("message").description("Response message"),
                            fieldWithPath("data.accessToken").description("New access token"),
                            fieldWithPath("data.refreshToken").description("New refresh token"),
                            fieldWithPath("data.userStatus").description("User status (ACTIVE, PENDING, BLOCKED, WITHDRAWN)")
                                .optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("Already active user gets bad request")
    fun activationFailWhenAlreadyActive() {
        val email = "active@example.com"
        val user = User.createWithPendingStatus(email, "Active User", OAuth2Provider.KAKAO).activate()
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser).copy(role = "PENDING")
        val initialToken = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${initialToken.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-already-active",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("Error code"),
                            fieldWithPath("message").description("Error message"),
                            fieldWithPath("data").description("No data").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("Authorization header가 없으면 401 Unauthorized를 반환한다")
    fun activationFailWhenNoAuthorizationHeader() {
        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("Invalid access token returns unauthorized")
    fun activationFailWhenInvalidToken() {
        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.invalid_signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-invalid-token",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("Error code"),
                            fieldWithPath("message").description("Error message"),
                            fieldWithPath("data").description("No data").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("필수 약관에 동의하지 않은 경우 활성화에 실패하고 예외를 반환하며 문서화한다")
    fun activationFailWhenMandatoryConsentMissing() {
        val email = "noconsent@example.com"
        val user = User.createWithPendingStatus(email, "No Consent User", OAuth2Provider.KAKAO)
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val initialToken = tokenProviderPort.issue(claims)

        val term = termService.save(
            TermCreateCommand(
                type = TermTypes.SERVICE,
                title = "필수 이용약관",
                content = "내용",
                effectiveDate = LocalDateTime.now().minusDays(1),
                isRequired = true
            )
        )

        // 필수 약관에 동의하지 않음
        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = term.id, isAgreed = false)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${initialToken.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest) // 비즈니스 정책에 따른 상태 코드
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-mandatory-consent",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (예: 약관 동의 누락)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID를 포함하여 요청 시 예외를 반환하며 문서화한다")
    fun activationFailWhenTermNotFound() {
        val email = "invalidterm@example.com"
        val user = User.createWithPendingStatus(email, "Invalid Term User", OAuth2Provider.KAKAO)
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val initialToken = tokenProviderPort.issue(claims)

        // 존재하지 않는 약관(예: id=999) 포함
        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 999L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${initialToken.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound) // 비즈니스 정책에 따른 상태 코드
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-term-not-found",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (예: 약관을 찾을 수 없음)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }
}
