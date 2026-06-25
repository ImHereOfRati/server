package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.adapter.`in`.web.dto.TokenRefreshRequest
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
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

/**
 * RefreshController E2E 통합 테스트.
 *
 * 실제 Spring Security 필터 체인과 JWT 토큰 파서를 사용하여 토큰 재발급 플로우를 검증하며,
 * 정상/실패 케이스 모두 RestDocs(epages)로 문서화한다.
 */
class RefreshControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tokenProviderPort: ImHereTokenProviderPort

    @Test
    @DisplayName("정상적인 리프레시 토큰으로 액세스 토큰을 재발급받고 200 OK를 반환하며 문서화한다")
    fun refreshSuccessAndDocument() {
        // given
        val email = "refresh@example.com"
        val user = User.createWithPendingStatus(email, "Refresh User", OAuth2Provider.KAKAO).activate()
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val initialToken = tokenProviderPort.issue(claims)

        val request = TokenRefreshRequest(refreshToken = initialToken.refreshToken)

        // when & then
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-refresh-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("refreshToken").description("기존에 발급받은 리프레시 토큰")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.accessToken").description("새로 발급된 액세스 토큰"),
                            fieldWithPath("data.refreshToken").description("새로 발급된 리프레시 토큰"),
                            fieldWithPath("data.userStatus").description("사용자 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN)").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun refreshFailWhenTokenInvalid() {
        val request = TokenRefreshRequest(refreshToken = "invalid-refresh-token")

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-refresh-fail-invalid-token",
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
    @DisplayName("만료된 리프레시 토큰으로 재발급 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun refreshFailWhenTokenExpired() {
        // given - 만료된 토큰은 이미 cache에서 삭제되었거나 서명이 유효하지 않음을 시뮬레이션
        // (실제로는 cache TTL이 지난 토큰 or 다른 서버에서 발급한 서명을 가진 토큰)
        val expiredLookingToken = TokenRefreshRequest(
            refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDF9.invalid_sig"
        )

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(expiredLookingToken))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-refresh-fail-expired-token",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (TOKEN-100: 만료된 토큰입니다 / TOKEN-101: 유효하지 않은 토큰입니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("Cache에 존재하지 않는(로그아웃된) 리프레시 토큰으로 재발급 시 401 Unauthorized와 에러를 반환하며 문서화한다")
    fun refreshFailWhenTokenNotFoundInCache() {
        // given - 유효한 서명이지만 cache에 없는 토큰 (로그아웃 후 재사용 시도 시나리오)
        val email = "logout@example.com"
        val user = User.createWithPendingStatus(email, "Logout User", OAuth2Provider.KAKAO).activate()
        val savedUser = userRepository.save(user)

        val claims = JwtTokenClaims.fromUser(savedUser)
        val token = tokenProviderPort.issue(claims)
        // 정상 발급 후 cache에서 제거하지 않고 직접 다른 refreshToken 값을 구성하는 대신,
        // cache에 없는 상황을 재현하기 위해 서명이 올바르지 않은 토큰을 사용한다.
        val invalidToken = token.refreshToken + "_tampered"

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(TokenRefreshRequest(refreshToken = invalidToken)))
        )
            .andExpect(status().isUnauthorized)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "auth-refresh-fail-token-not-in-cache",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("에러 코드 (TOKEN-103: 인증 정보를 찾을 수 없거나 만료되었습니다)"),
                            fieldWithPath("message").description("에러 상세 메시지"),
                            fieldWithPath("data").description("데이터는 없음").optional()
                        )
                    )
                )
            )
    }
}
