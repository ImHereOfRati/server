package com.kdongsu5509.user.adapter.`in`.web.user

import com.common.testUtil.ControllerTestSupport
import com.common.testUtil.TestJwtBuilder
import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.kdongsu5509.user.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOauthClient
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import com.kdongsu5509.user.domain.terms.TermsTypes
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.interfaces.RSAPublicKey
import java.time.Duration.ofMinutes
import java.time.LocalDateTime
import java.util.*

class UserAgreementControllerIntegrationTest : ControllerTestSupport() {

    @Autowired
    lateinit var userRepository: SpringDataUserRepository

    @Autowired
    lateinit var termsDefinitionRepository: SpringDataTermsDefinitionRepository

    @Autowired
    lateinit var termsVersionRepository: SpringDataTermsVersionRepository

    @Autowired
    lateinit var cachePort: CachePort

    @MockitoBean
    lateinit var kakaoOauthClient: KakaoOauthClient

    @Value("\${oidc.kakao.cache-key}")
    lateinit var kakaoCacheKey: String

    companion object {
        const val BASE_URL = "/api/user/terms"
        const val CONSENT_ALL_URL = "/consent"
        const val CONSENT_SINGLE_URL = "/consent/{termDefinitionId}"
        const val TEST_USER = "ds.ko@kakao.com" // 로그의 이메일과 맞춤
        const val TEST_NICKNAME = "rati"
        var TEST_DEF_ID1 = 0L
        var TEST_DEF_ID2 = 0L
        var TEST_DEF_ID3 = 0L
    }

    @BeforeEach
    fun setUp() {
        setMockKakaoPublicKey()
        userRepository.deleteAll()
        userRepository.save(
            UserJpaEntity(TEST_USER, TEST_NICKNAME, UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.PENDING)
        )

        val testTermDef1 = termsDefinitionRepository.save(TermsDefinitionJpaEntity("약관1", TermsTypes.LOCATION, true))
        val testTermDef2 = termsDefinitionRepository.save(TermsDefinitionJpaEntity("약관2", TermsTypes.PRIVACY, true))
        val testTermDef3 = termsDefinitionRepository.save(TermsDefinitionJpaEntity("약관3", TermsTypes.MARKETING, false))

        TEST_DEF_ID1 = testTermDef1.id!!
        TEST_DEF_ID2 = testTermDef2.id!!
        TEST_DEF_ID3 = testTermDef3.id!!

        listOf(testTermDef1, testTermDef2, testTermDef3).forEach { createTestTermVersionEntity(it) }
    }

    @Test
    @DisplayName("전체 약관 동의 API가 정상적으로 호출된다")
    fun consentAll_success() {
        val refreshToken = TestJwtBuilder.buildImHereRefreshToken(TEST_USER, TEST_NICKNAME)
        val accessToken = TestJwtBuilder.buildImHereAccessToken(TEST_USER, TEST_NICKNAME)

        val redisKey = "refresh:$TEST_USER"
        cachePort.save(redisKey, refreshToken, ofMinutes(30))

        val simpleTokenUserDetails = SimpleTokenUserDetails(
            email = TEST_USER, nickname = TEST_NICKNAME, role = "ROLE_NORMAL", status = "PENDING"
        )

        val request = UserTermsConsentRequest(
            consents = listOf(
                UserTermsConsentRequest.ConsentDetail(TEST_DEF_ID1, true),
                UserTermsConsentRequest.ConsentDetail(TEST_DEF_ID2, true),
                UserTermsConsentRequest.ConsentDetail(TEST_DEF_ID3, true)
            )
        )

        mockMvc.perform(
            post(BASE_URL + CONSENT_ALL_URL)
                .header("Authorization", "Bearer $accessToken")
                .with(user(simpleTokenUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andDo(
                document(
                    "terms-consent-all",
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("약관 동의")
                            .summary("전체 약관 일괄 동의")
                            .description(
                                """
                                PENDING 상태의 사용자가 약관에 동의합니다.
                                **모든 필수(required=true) 약관에 동의 완료 시 사용자 상태가 PENDING → ACTIVE로 자동 전환됩니다.**
                                전환 후 새로운 accessToken/refreshToken이 재발급되어 응답에 포함됩니다.
                                """.trimIndent()
                            )
                            .build()
                    )
                )
            )
    }

    private fun createTestTermVersionEntity(testTermDef: TermsDefinitionJpaEntity) {
        termsVersionRepository.save(
            TermsVersionJpaEntity("1.0", "내용", true, LocalDateTime.now().plusDays(1), testTermDef)
        )
    }

    private fun setMockKakaoPublicKey() {
        val rsaPublicKey = TestJwtBuilder.testPublicKey as RSAPublicKey
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val mockKey = OIDCPublicKey(
            kid = TestJwtBuilder.KAKAO_HEADER_KID, kty = "RSA", alg = TestJwtBuilder.KAKAO_HEADER_ALG, use = "sig",
            n = encoder.encodeToString(rsaPublicKey.modulus.toByteArray()),
            e = encoder.encodeToString(rsaPublicKey.publicExponent.toByteArray())
        )
        val mockResponse = OIDCPublicKeyResponse(keys = listOf(mockKey))
        `when`(kakaoOauthClient.getPublicKeyFromProvider()).thenReturn(mockResponse)
        redisTemplate.opsForValue().set(kakaoCacheKey, mockResponse)
    }
}
