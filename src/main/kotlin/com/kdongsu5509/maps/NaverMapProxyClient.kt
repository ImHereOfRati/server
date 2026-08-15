package com.kdongsu5509.maps

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Duration

@Component
class NaverMapProxyClient(
    restClientBuilder: RestClient.Builder,
    @Value("\${naver.map.client-id}") private val mapClientId: String,
    @Value("\${naver.map.client-secret}") private val mapClientSecret: String,
    @Value("\${naver.search.client-id}") private val searchClientId: String,
    @Value("\${naver.search.client-secret}") private val searchClientSecret: String,
) {
    private val mapsClient = restClientBuilder.clone()
        .baseUrl(MAPS_BASE_URL)
        .requestFactory(requestFactory())
        .defaultHeader("x-ncp-apigw-api-key-id", mapClientId)
        .defaultHeader("x-ncp-apigw-api-key", mapClientSecret)
        .build()

    private val searchClient = restClientBuilder.clone()
        .baseUrl(SEARCH_BASE_URL)
        .requestFactory(requestFactory())
        .defaultHeader("X-Naver-Client-Id", searchClientId)
        .defaultHeader("X-Naver-Client-Secret", searchClientSecret)
        .build()

    fun geocode(query: String): Map<String, Any?> {
        requireCredentials(mapClientId, mapClientSecret)
        return execute {
            mapsClient.get()
                .uri { it.path(GEOCODE_PATH).queryParam("query", query).build() }
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(Map::class.java)
        }
    }

    fun reverseGeocode(latitude: Double, longitude: Double): Map<String, Any?> {
        requireCredentials(mapClientId, mapClientSecret)
        return execute {
            mapsClient.get()
                .uri {
                    it.path(REVERSE_GEOCODE_PATH)
                        .queryParam("coords", "$longitude,$latitude")
                        .queryParam("orders", "roadaddr,addr")
                        .queryParam("output", "json")
                        .build()
                }
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(Map::class.java)
        }
    }

    fun searchLocal(query: String, display: Int): Map<String, Any?> {
        requireCredentials(searchClientId, searchClientSecret)
        return execute {
            searchClient.get()
                .uri {
                    it.path(LOCAL_SEARCH_PATH)
                        .queryParam("query", query)
                        .queryParam("display", display)
                        .build()
                }
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .body(Map::class.java)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun execute(block: () -> Map<*, *>?): Map<String, Any?> {
        try {
            return block()?.entries?.associate { it.key.toString() to it.value }
                ?: emptyMap()
        } catch (error: ResourceAccessException) {
            throw NaverMapProxyException(
                HttpStatus.GATEWAY_TIMEOUT,
                "MAP-504",
                "지도 서비스 응답 시간이 초과되었습니다.",
                error,
            )
        } catch (error: RestClientException) {
            throw NaverMapProxyException(
                HttpStatus.BAD_GATEWAY,
                "MAP-502",
                "지도 서비스에 연결할 수 없습니다.",
                error,
            )
        }
    }

    private fun requireCredentials(clientId: String, secret: String) {
        if (clientId.isBlank() || secret.isBlank()) {
            throw NaverMapProxyException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MAP-503",
                "지도 서비스 설정을 확인해 주세요.",
            )
        }
    }

    private fun requestFactory() = SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(Duration.ofSeconds(3))
        setReadTimeout(Duration.ofSeconds(5))
    }

    companion object {
        private const val MAPS_BASE_URL = "https://maps.apigw.ntruss.com"
        private const val SEARCH_BASE_URL = "https://openapi.naver.com"
        private const val GEOCODE_PATH = "/map-geocode/v2/geocode"
        private const val REVERSE_GEOCODE_PATH = "/map-reversegeocode/v2/gc"
        private const val LOCAL_SEARCH_PATH = "/v1/search/local.json"
    }
}
