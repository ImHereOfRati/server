package com.kdongsu5509.user.adapter.out.auth.oauth

import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.application.port.out.user.oauth.OauthClientPort
import com.kdongsu5509.user.application.service.user.auth.KakaoPublicKeyScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import tools.jackson.databind.json.JsonMapper

@ActiveProfiles("test")
@RestClientTest(KakaoOauthClient::class)
@Import(KakaoOauthClientTest.Config::class)
class KakaoOauthClientTest {

    @TestConfiguration
    class Config {
        @Bean
        fun kakaoPublicKeyApiClient(restClientBuilder: RestClient.Builder): KakaoOauthPublicKeyApiClient {
            val restClient = restClientBuilder
                .baseUrl("https://kauth.kakao.com")
                .build()
            return HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)
            ).build().createClient<KakaoOauthPublicKeyApiClient>()
        }
    }

    companion object {
        const val KAKAO_URL = "https://kauth.kakao.com/.well-known/jwks.json"
        val mockResponseJson = createMockKakaoPublicKeyResp()

        private fun createMockKakaoPublicKeyResp(): String {
            val mockResponse = OIDCPublicKeyResponse(
                keys = listOf(
                    OIDCPublicKey("kid1", "RSA", "sig", "n_value", "e_value")
                )
            )
            return JsonMapper().writeValueAsString(mockResponse)
        }
    }

    @Autowired
    private lateinit var mockServer: MockRestServiceServer

    @Autowired
    private lateinit var oauthClientPort: OauthClientPort

    @MockitoBean
    private lateinit var kakaoPublicKeyScheduler: KakaoPublicKeyScheduler

    @MockitoBean(name = "oidcCacheManager")
    private lateinit var oidcCacheManager: CacheManager

    @Test
    @DisplayName("카카??공개???�청 ???�상?�으�??�이?��? ?�싱?�서 반환?�다")
    fun fetchPublicKey_success() {
        // given
        mockServer.expect(requestTo(KAKAO_URL))
            .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON))

        // when
        val result = oauthClientPort.fetchPublicKey()

        // then
        assertThat(result).isNotNull
        assertThat(result?.keys).hasSize(1)
        assertThat(result?.keys?.get(0)?.kid).isEqualTo("kid1")

        mockServer.verify()
    }
}
