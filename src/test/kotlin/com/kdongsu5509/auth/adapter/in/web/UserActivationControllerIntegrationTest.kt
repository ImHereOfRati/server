package com.kdongsu5509.auth.adapter.`in`.web

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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * UserActivationController E2E 통합 테스트.
 *
 * 실제 Spring Security 필터 체인(JWT 인증 + Method Security)과 DB를 사용하여
 * 사용자 활성화(가입 완료) 플로우를 검증하며, 정상/실패 케이스 모두 RestDocs(epages)로 문서화한다.
 */
@SpringBootTest
class UserActivationControllerIntegrationTest : AuthIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProviderPort: ImHereTokenProviderPort

    @Test
    @DisplayName("PENDING 상태의 사용자가 약관 동의와 함께 활성화(가입 완료) 요청을 하면 ACTIVE 상태가 되고 200 OK와 새 토큰을 반환하며 문서화한다")
    fun activationSuccessAndDocument() {
        // given
        val email = "pending@example.com"
        val user = User.createWithPendingStatus(email, "Pending User", OAuth2Provider.KAKAO)
        userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(user)
        val initialToken = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true),
                UserActivationRequest.TermConsent(id = 2L, isAgreed = false)
            )
        )

        // when & then
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
                            fieldWithPath("consents").description("약관 동의 내역 목록"),
                            fieldWithPath("consents[].id").description("약관 ID"),
                            fieldWithPath("consents[].agreed").description("동의 여부")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("활성화 후 새로 발급된 액세스 토큰 (권한 변경됨)"),
                            fieldWithPath("data.refreshToken").description("새로 발급된 리프레시 토큰")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("이미 활성화된(ACTIVE) 사용자가 다시 활성화를 요청하면 400 Bad Request와 에러를 반환하며 문서화한다")
    fun activationFailWhenAlreadyActive() {
        // given
        val email = "active@example.com"
        val user = User.createWithPendingStatus(email, "Active User", OAuth2Provider.KAKAO).activate()
        userRepository.save(user)

        // Token issued with PENDING role (simulating an old token before status update)
        val claims = JwtTokenClaims.fromUser(user).copy(role = "PENDING")
        val initialToken = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        // when & then
        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer ${initialToken.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-already-active",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (예: USER-001: 이미 활성화된 사용자)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("Authorization 헤더 없이 활성화 요청 시 401 Unauthorized와 에러를 반환하며 문서화한다")
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
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-no-token",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (인증 토큰 누락)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("유효하지 않은 액세스 토큰으로 활성화 요청 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun activationFailWhenInvalidToken() {
        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        mockMvc.perform(
            post("/api/auth/activation")
                .header("Authorization", "Bearer invalid.jwt.token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-activation-fail-invalid-token",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (TOKEN-101: 유효하지 않은 토큰입니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("ACTIVE 권한 사용자가 활성화 엔드포인트에 접근하면 403 Forbidden을 반환하며 문서화한다")
    fun activationFailWhenNotPendingRole() {
        // given - ACTIVE 상태 사용자의 정상 토큰 (PENDING 권한이 없음)
        val email = "normaluser@example.com"
        val user = User.createWithPendingStatus(email, "Normal User", OAuth2Provider.KAKAO).activate()
        userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(user) // NORMAL role
        val token = tokenProviderPort.issue(claims)

        val request = UserActivationRequest(
            consents = listOf(
                UserActivationRequest.TermConsent(id = 1L, isAgreed = true)
            )
        )

        // when & then
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
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-200: 해당 기능에 대한 권한이 없습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }
}
