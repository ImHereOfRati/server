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
 * RegistrationController E2E 통합 테스트.
 *
 * 실제 Spring Security 필터 체인과 DB를 사용하여 회원가입 플로우를 검증하며,
 * 정상/실패 케이스 모두 RestDocs(epages)로 문서화한다.
 */
class RegistrationControllerIntegrationTest : WebIntegrationTestSupport() {

    @MockitoBean
    private lateinit var oidcVerifyPort: OIDCVerifyPort

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    @DisplayName("정상적으로 회원가입을 수행하고 201 Created를 반환하며 문서화한다")
    fun registerSuccessAndDocument() {
        // given
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token")

        given(oidcVerifyPort.verify(any(), any())).willReturn(
            OIDCUserInfo(email = "newuser@example.com", nickname = "New User")
        )

        // when & then
        mockMvc.perform(
            post("/api/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-registration-success",
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
    @DisplayName("만료된 OIDC 토큰으로 회원가입 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun registerFailWhenOidcExpired() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "expired-id-token")

        given(oidcVerifyPort.verify(any(), any())).willAnswer {
            AuthException.OIDC_EXPIRED.throwIt()
        }

        mockMvc.perform(
            post("/api/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-registration-fail-oidc-expired",
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
    @DisplayName("OIDC 토큰 형식이 올바르지 않으면 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun registerFailWhenOidcFormatInvalid() {
        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "malformed-token")

        given(oidcVerifyPort.verify(any(), any())).willAnswer {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }

        mockMvc.perform(
            post("/api/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-registration-fail-oidc-format-invalid",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (AUTH-101: OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("이미 가입된 이메일로 재가입 시도 시 적절한 에러를 반환하며 문서화한다")
    fun registerFailWhenAlreadyRegistered() {
        // given
        val email = "existing@example.com"
        val existingUser = User.createWithPendingStatus(email, "Existing User", OAuth2Provider.KAKAO).activate()
        userRepository.save(existingUser)

        val request = OIDCAuthRequest(provider = OAuth2Provider.KAKAO, idToken = "valid-id-token")

        given(oidcVerifyPort.verify(any(), any())).willReturn(
            OIDCUserInfo(email = email, nickname = "Existing User")
        )

        // when & then
        mockMvc.perform(
            post("/api/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().is4xxClientError)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-registration-fail-already-registered",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (이미 가입된 사용자)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }
}
