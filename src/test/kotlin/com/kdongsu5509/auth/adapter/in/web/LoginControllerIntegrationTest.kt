package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * LoginController E2E 통합 테스트.
 *
 * 실제 Spring Security 필터 체인과 DB를 사용하여 로그인 플로우를 검증하며,
 * 정상/실패 케이스 모두 RestDocs(epages)로 문서화한다.
 */
class LoginControllerIntegrationTest : WebIntegrationTestSupport() {

    companion object {
        private const val NONCE = "test-nonce"
    }

    @MockitoBean
    private lateinit var oidcVerifyPort: OIDCVerifyPort

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("정상적으로 로그인을 수행하고 200 OK와 토큰을 반환하며 문서화한다")
    fun loginSuccessAndDocument() {
        // given
        val email = "test@example.com"
        val user = User.createWithPendingStatus(email, "Test User", OAuth2Provider.KAKAO)
        userRepository.save(user.activate())

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Test User")
        )

        // when & then
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("provider").description("OAuth2 제공자 (예: KAKAO, APPLE)"),
                            fieldWithPath("idToken").description("OIDC ID 토큰"),
                            fieldWithPath("nonce").description("OIDC nonce")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("발급된 액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("발급된 리프레시 토큰"),
                            fieldWithPath("data.userStatus").description("사용자 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN)").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("로그인 시 등록되지 않은 사용자면 404 NotFound와 에러를 반환하며 문서화한다")
    fun loginFailWhenUserNotRegistered() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = "notfound@example.com", nickname = "Test User")
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-not-registered",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-300: 사용자 정보를 찾을 수 없습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("만료된 OIDC 토큰으로 로그인 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun loginFailWhenOidcTokenExpired() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "expired-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willAnswer {
            AuthException.OIDC_EXPIRED.throwIt()
        }

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-oidc-expired",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-100: OIDC ID 토큰이 만료되었습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("OIDC 토큰 서명 검증 실패 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun loginFailWhenOidcSignatureInvalid() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "tampered-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willAnswer {
            AuthException.OIDC_SIGNATURE_INVALID.throwIt()
        }

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-oidc-signature-invalid",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-102: OIDC ID 토큰의 서명 검증에 실패했습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("정지된 계정(BLOCKED)으로 로그인 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun loginFailWhenUserBlocked() {
        val email = "blocked@example.com"
        val user = User.createWithPendingStatus(email, "Blocked User", OAuth2Provider.KAKAO).activate().block()
        userRepository.save(user)

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Blocked User")
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-blocked",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-105: 비활성화된 계정입니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("가입 대기 중인 계정(PENDING)으로 로그인 시 200 OK와 토큰, 상태를 반환하며 문서화한다")
    fun loginSuccessWhenUserPending() {
        val email = "pending@example.com"
        val user = User.createWithPendingStatus(email, "Pending User", OAuth2Provider.KAKAO)
        userRepository.save(user)

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Pending User")
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-success-pending",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("발급된 액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("발급된 리프레시 토큰"),
                            fieldWithPath("data.userStatus").description("사용자 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN)")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("탈퇴한 계정(WITHDRAWN)으로 로그인 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun loginFailWhenUserWithdrawn() {
        val email = "withdrawn@example.com"
        val user = User.createWithPendingStatus(email, "Withdrawn User", OAuth2Provider.KAKAO).activate().withdraw()
        userRepository.save(user)

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token", nonce = NONCE)

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Withdrawn User")
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-withdrawn",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-108: 탈퇴한 계정입니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }
}
