package com.kdongsu5509.imhereuserservice.adapter.`in`.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhereuserservice.TestJwtBuilder
import com.kdongsu5509.imhereuserservice.TestRedisContainer
import com.kdongsu5509.imhereuserservice.adapter.out.dto.OIDCPublicKey
import com.kdongsu5509.imhereuserservice.adapter.out.dto.OIDCPublicKeyResponse
import com.kdongsu5509.imhereuserservice.adapter.out.kakao.KakaoOauthClient
import com.kdongsu5509.imhereuserservice.application.port.`in`.HandleOIDCUseCase
import com.kdongsu5509.imhereuserservice.application.port.`in`.ReissueJWTPort
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
import java.security.interfaces.RSAPublicKey
import java.util.*

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest : TestRedisContainer() {

    companion object {
        const val LOGIN_URL = "/api/v1/user/login"
        const val REISSUE_URL = "/api/v1/user/reissue"
    }

    @Autowired
    lateinit var handleOidcUseCase: HandleOIDCUseCase

    @Autowired
    lateinit var reissueJWTPort: ReissueJWTPort

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
    @DisplayName("kakao oauth를 이용해 잘 로그인 한다")
    fun login_success_with_kakao_oauth_oidc() {
        val kakaoOidcToken = TestJwtBuilder.buildValidIdToken()
        val testMockTokenInfo = objectMapper.writeValueAsString(
            mapOf(
                "provider" to "KAKAO",
                "idToken" to kakaoOidcToken
            )
        )

        mockMvc.perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(testMockTokenInfo)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    @Test
    @DisplayName("잘못된 토큰에 대해서는 오류 코드를 반환한다.")
    fun login_fail_with_invalid_token() {
        val mockToken = "invalid_token"
        val testMockTokenInfo = objectMapper.writeValueAsString(
            mapOf(
                "provider" to "KAKAO",
                "idToken" to mockToken
            )
        )


        mockMvc.perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(testMockTokenInfo)
        )
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.data.code").value("AUTH_COMMON_001"))
            .andExpect(jsonPath("$.data.message").value("토큰 형식이 올바르지 않습니다."))
    }

    @Test
    @DisplayName("정상적인 refreshToken은 잘 통과한다.")
    fun reissue_okay() {
        val testRefreshToken = obtainRefreshToken()
        val testJwtRefreshToken = objectMapper.writeValueAsString(
            mapOf(
                "refreshToken" to testRefreshToken
            )
        )

        mockMvc.perform(
            post(REISSUE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(testJwtRefreshToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
    }

    private fun setMockKakaoPublicKey() {
        val mockResponse = OIDCPublicKeyResponse(
            keys = listOf(
                createMockPublicKey()
            )
        )

        `when`(kakaoOauthClient.getPublicKeyFromProvider()).thenReturn(mockResponse)

        redisTemplate.opsForValue().set(kakaoCacheKey, mockResponse)
    }

    private fun createMockPublicKey(): OIDCPublicKey {
        val rsaPublicKey = TestJwtBuilder.testPublicKey as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val n = encoder.encodeToString(rsaPublicKey.modulus.toByteArray())
        val e = encoder.encodeToString(rsaPublicKey.publicExponent.toByteArray())

        val oidcPublicKey = OIDCPublicKey(
            kid = TestJwtBuilder.KAKAO_HEADER_KID,
            kty = "RSA",
            alg = TestJwtBuilder.KAKAO_HEADER_ALG,
            use = "sig",
            n = n,
            e = e
        )
        return oidcPublicKey
    }

    private fun obtainRefreshToken(): String {
        val kakaoOidcToken = TestJwtBuilder.buildValidIdToken()
        val loginRequestBody = objectMapper.writeValueAsString(
            mapOf("provider" to "KAKAO", "idToken" to kakaoOidcToken)
        )

        val result = mockMvc.perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginRequestBody)
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseMap = objectMapper.readValue(result.response.contentAsString, Map::class.java)
        val dataMap = responseMap["data"] as Map<*, *>
        return dataMap["refreshToken"].toString()
    }
}