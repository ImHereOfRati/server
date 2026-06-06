package com.kdongsu5509.auth.adapter.`in`.web

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.`in`.web.dto.OIDCAuthRequest
import com.kdongsu5509.auth.application.OIDCUserInfo
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.common.testsupport.WebIntegrationTestSupport
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
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

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token")

        given(oidcVerifyPort.verify(any(), any())).willReturn(
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
                            fieldWithPath("idToken").description("OIDC ID 토큰")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("발급된 액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("발급된 리프레시 토큰")
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("로그인 시 등록되지 않은 사용자면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun loginFailWhenUserNotRegistered() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token")

        given(oidcVerifyPort.verify(any(), any())).willReturn(
            OIDCUserInfo(email = "notfound@example.com", nickname = "Test User")
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-login-fail-not-registered",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-104: 사용자 정보를 찾을 수 없습니다)"),
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
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "expired-id-token")

        given(oidcVerifyPort.verify(any(), any())).willAnswer {
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
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "tampered-id-token")

        given(oidcVerifyPort.verify(any(), any())).willAnswer {
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
}
