package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhereuserservice.adapter.out.auth.oauth.KakaoOauthClient
import com.kdongsu5509.imhereuserservice.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.imhereuserservice.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserSavePort
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import com.kdongsu5509.imhereuserservice.testSupport.TestJwtBuilder
import com.kdongsu5509.imhereuserservice.testSupport.TestRedisContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.security.interfaces.RSAPublicKey
import java.util.*

@ActiveProfiles("test")
@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest : TestRedisContainer() {

    companion object {
        const val LOGIN_URL = "/api/v1/user/auth/login"
        const val REISSUE_URL = "/api/v1/user/auth/reissue"
        const val DEFAULT_TEST_EMAIL = "ds.ko@kakao.com"
    }

    @Autowired
    lateinit var userSavePort: UserSavePort

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var redisTemplate: RedisTemplate<String, Any>

    @MockitoBean
    lateinit var kakaoOauthClient: KakaoOauthClient

    @Value("\${oidc.kakao.cache-key}")
    lateinit var kakaoCacheKey: String

    @BeforeEach
    fun setUp() {
        setMockKakaoPublicKey()
    }

    @Test
    @DisplayName("신규 유저는 kakao oauth를 통해 201 상태코드로 가입된다")
    fun login_success_new_member() {
        // given
        val idToken = TestJwtBuilder.buildValidIdTokenWithCustomEmail("new-user@kakao.com")

        // when & then
        performLogin(idToken)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    @Test
    @DisplayName("PENDING인 사용자도 201 상태코드를 반환한다")
    fun login_success_pending_member() {
        // given
        val email = "pending@kakao.com"
        saveUser(email, UserStatus.PENDING)
        val idToken = TestJwtBuilder.buildValidIdTokenWithCustomEmail(email)

        // when & then
        performLogin(idToken)
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    @Test
    @DisplayName("기존 유저는 kakao oauth를 통해 200 상태코드로 로그인된다")
    fun login_success_existing_member() {
        // given
        saveUser(DEFAULT_TEST_EMAIL, UserStatus.ACTIVE)
        val idToken = TestJwtBuilder.buildValidIdToken()

        // when & then
        // 1. 첫 로그인
        performLogin(idToken).andExpect(status().isOk)

        // 2. 재로그인 (동일 정보)
        performLogin(idToken)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    @Test
    @DisplayName("잘못된 토큰에 대해서는 오류 코드를 반환한다.")
    fun login_fail_with_invalid_token() {
        // given
        val invalidToken = "invalid_token"

        // when & then
        performLogin(invalidToken)
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.code").value(400)) // APIResponse의 code 필드 확인
            .andExpect(jsonPath("$.data.message").value(ErrorCode.OIDC_INVALID.message))
    }

    @Test
    @DisplayName("정상적인 refreshToken은 잘 통과한다.")
    fun reissue_okay() {
        // given
        val refreshToken = obtainRefreshToken()
        val requestBody = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))

        // when & then
        mockMvc.perform(
            post(REISSUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").exists())
    }

    // --- Helper Methods ---

    private fun performLogin(idToken: String) = mockMvc.perform(
        post(LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                objectMapper.writeValueAsString(
                    mapOf(
                        "provider" to "KAKAO",
                        "idToken" to idToken
                    )
                )
            )
    )

    private fun saveUser(email: String, status: UserStatus) {
        userSavePort.save(
            User(UUID.randomUUID(), email, "테스터", OAuth2Provider.KAKAO, UserRole.NORMAL, status = status)
        )
    }

    private fun obtainRefreshToken(): String {
        val idToken = TestJwtBuilder.buildValidIdToken()
        val response = performLogin(idToken).andReturn().response.contentAsString

        return objectMapper.readTree(response).path("data").path("refreshToken").asText()
    }

    private fun setMockKakaoPublicKey() {
        val rsaPublicKey = TestJwtBuilder.testPublicKey as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()

        val mockKey = OIDCPublicKey(
            kid = TestJwtBuilder.KAKAO_HEADER_KID,
            kty = "RSA",
            alg = TestJwtBuilder.KAKAO_HEADER_ALG,
            use = "sig",
            n = encoder.encodeToString(rsaPublicKey.modulus.toByteArray()),
            e = encoder.encodeToString(rsaPublicKey.publicExponent.toByteArray())
        )

        val mockResponse = OIDCPublicKeyResponse(keys = listOf(mockKey))
        `when`(kakaoOauthClient.getPublicKeyFromProvider()).thenReturn(mockResponse)
        redisTemplate.opsForValue().set(kakaoCacheKey, mockResponse)
    }
}