package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * AuthController E2E 통합 테스트.
 *
 * 실제 Spring Security 필터 체인과 DB를 사용하여 OIDC 인증 플로우를 검증하며,
 * 정상/실패 케이스 모두 RestDocs(epages)로 문서화한다.
 *
 * `/api/auth`는 가입과 로그인을 구분하지 않는다. 계정이 없으면 만들고, 있으면 그대로 로그인시킨다.
 */
class AuthControllerIntegrationTest : WebIntegrationTestSupport() {

    companion object {
        private const val REQUEST_API = "/api/auth"
        private const val NONCE = "test-nonce"
    }

    @MockitoBean
    private lateinit var oidcVerifyPort: OIDCVerifyPort

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("신규 사용자면 계정을 만들고 200 OK와 토큰을 반환하며 문서화한다")
    fun authCreatesNewUserAndDocument() {
        // given
        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = "newuser@example.com", nickname = "New User")
        )

        // when & then
        mockMvc.perform(authRequest())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userStatus").value(UserStatus.PENDING.name))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-success-new-user",
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
                            fieldWithPath("data.userStatus").description("사용자 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN). 신규 가입은 PENDING")
                                .optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("이미 가입된 사용자면 중복 에러 없이 로그인 처리되어 200 OK와 토큰을 반환하며 문서화한다")
    fun authLogsInExistingUserAndDocument() {
        // given
        val email = "existing@example.com"
        userRepository.save(User(email, "Existing User", OAuth2Provider.KAKAO).activate())

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Existing User")
        )

        // when & then
        mockMvc.perform(authRequest())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.data.userStatus").value(UserStatus.ACTIVE.name))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-success-existing-user",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("발급된 액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("발급된 리프레시 토큰"),
                            fieldWithPath("data.userStatus").description("기존 계정의 상태").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("가입 대기 중인 계정(PENDING)도 200 OK와 토큰, 상태를 반환하며 문서화한다")
    fun authSucceedsWhenUserPending() {
        // given
        val email = "pending@example.com"
        userRepository.save(User(email, "Pending User", OAuth2Provider.KAKAO))

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Pending User")
        )

        // when & then
        mockMvc.perform(authRequest())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.userStatus").value(UserStatus.PENDING.name))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-success-pending",
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
    @DisplayName("만료된 OIDC 토큰이면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun authFailWhenOidcExpired() {
        given(oidcVerifyPort.verify(any(), any(), any())).willAnswer {
            AuthException.OIDC_EXPIRED.throwIt()
        }

        mockMvc.perform(authRequest(idToken = "expired-id-token"))
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-fail-oidc-expired",
                    snippets = arrayOf(errorResponseFields("AUTH-100: OIDC ID 토큰이 만료되었습니다"))
                )
            )
    }

    @Test
    @DisplayName("OIDC 토큰 형식이 올바르지 않으면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun authFailWhenOidcFormatInvalid() {
        given(oidcVerifyPort.verify(any(), any(), any())).willAnswer {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }

        mockMvc.perform(authRequest(idToken = "malformed-token"))
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-fail-oidc-format-invalid",
                    snippets = arrayOf(errorResponseFields("AUTH-101: OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다"))
                )
            )
    }

    @Test
    @DisplayName("OIDC 토큰 서명 검증에 실패하면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun authFailWhenOidcSignatureInvalid() {
        given(oidcVerifyPort.verify(any(), any(), any())).willAnswer {
            AuthException.OIDC_SIGNATURE_INVALID.throwIt()
        }

        mockMvc.perform(authRequest(idToken = "tampered-id-token"))
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-fail-oidc-signature-invalid",
                    snippets = arrayOf(errorResponseFields("AUTH-102: OIDC ID 토큰의 서명 검증에 실패했습니다"))
                )
            )
    }

    @Test
    @DisplayName("정지된 계정(BLOCKED)이면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun authFailWhenUserBlocked() {
        // given
        val email = "blocked@example.com"
        userRepository.save(User(email, "Blocked User", OAuth2Provider.KAKAO).activate().block())

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Blocked User")
        )

        // when & then
        mockMvc.perform(authRequest())
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-fail-blocked",
                    snippets = arrayOf(errorResponseFields("AUTH-105: 비활성화된 계정입니다"))
                )
            )
    }

    @Test
    @DisplayName("탈퇴한 계정(WITHDRAWN)이면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun authFailWhenUserWithdrawn() {
        // given
        val email = "withdrawn@example.com"
        userRepository.save(User(email, "Withdrawn User", OAuth2Provider.KAKAO).activate().withdraw())

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Withdrawn User")
        )

        // when & then
        mockMvc.perform(authRequest())
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-fail-withdrawn",
                    snippets = arrayOf(errorResponseFields("AUTH-108: 탈퇴한 계정입니다"))
                )
            )
    }

    @Test
    @DisplayName("인증 API는 관리자 IP allowlist의 영향을 받지 않는다")
    fun authIgnoresAdminIpAllowlist() {
        // given
        val email = "public-login@example.com"
        userRepository.save(User(email, "Public Login User", OAuth2Provider.KAKAO).activate())

        given(oidcVerifyPort.verify(any(), any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Public Login User")
        )

        // when & then
        mockMvc.perform(authRequest().header("X-Real-IP", "198.51.100.7"))
            .andExpect(status().isOk)
    }

    private fun authRequest(idToken: String = "valid-id-token") =
        post(REQUEST_API)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                jsonMapper.writeValueAsString(
                    OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = idToken, nonce = NONCE)
                )
            )

    private fun errorResponseFields(errorCodeDescription: String) = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드 ($errorCodeDescription)"),
        fieldWithPath("message").description("에러 상세 메시지"),
        fieldWithPath("data").description("데이터는 없음").optional()
    )
}
