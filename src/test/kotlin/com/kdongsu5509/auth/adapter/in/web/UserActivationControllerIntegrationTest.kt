package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.adapter.`in`.web.dto.UserActivationRequest
import com.kdongsu5509.auth.application.JwtTokenClaims
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class UserActivationControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProviderPort: ImHereTokenProviderPort

    @Test
    @DisplayName("PENDING user can activate successfully")
    fun activationSuccessAndDocument() {
        val email = "pending@example.com"
        val user = User.createWithPendingStatus(email, "Pending User", OAuth2Provider.KAKAO)
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val initialToken = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true),
                UserActivationRequest.TermConsent(id = 2L, isAgreed = false)
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
                            fieldWithPath("data.refreshToken").description("New refresh token")
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
    @DisplayName("Authorization header가 없으면 스프링 시큐리티 필터가 403를 던진다")
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
            .andExpect(status().isForbidden)
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
    @DisplayName("Active user without PENDING role gets forbidden")
    fun activationFailWhenNotPendingRole() {
        val email = "normaluser@example.com"
        val user = User.createWithPendingStatus(email, "Normal User", OAuth2Provider.KAKAO).activate()
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val token = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${token.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-forbidden",
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
}
