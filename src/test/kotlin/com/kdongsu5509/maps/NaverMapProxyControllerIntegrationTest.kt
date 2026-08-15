package com.kdongsu5509.maps

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NaverMapProxyControllerIntegrationTest : WebIntegrationTestSupport() {

    @MockitoBean
    private lateinit var mapService: NaverMapProxyService

    private val principal = ImHereUserDetails(
        email = "map-user@example.com",
        nickname = "map-user",
        role = "USER",
        status = "ACTIVE",
        userId = java.util.UUID.randomUUID()
    )

    @Test
    fun geocode_success_is_documented() {
        given(mapService.geocode(any())).willReturn(mapOf("addresses" to emptyList<Any>()))

        mockMvc.perform(get("/api/maps/geocode").param("query", "Seoul").with(user(principal)))
            .andExpect(status().isOk)
            .andDo(documentation("map-geocode-success"))
    }

    @Test
    fun geocode_blank_query_fails() {
        mockMvc.perform(get("/api/maps/geocode").param("query", "").with(user(principal)))
            .andExpect(status().isBadRequest)
            .andDo(documentation("map-geocode-bad-request"))
    }

    @Test
    fun reverse_geocode_success_is_documented() {
        given(mapService.reverseGeocode(any(), any())).willReturn(mapOf("results" to emptyList<Any>()))

        mockMvc.perform(
            get("/api/maps/reverse-geocode")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .with(user(principal))
        ).andExpect(status().isOk)
            .andDo(documentation("map-reverse-geocode-success"))
    }

    @Test
    fun reverse_geocode_out_of_range_fails() {
        mockMvc.perform(
            get("/api/maps/reverse-geocode")
                .param("latitude", "91")
                .param("longitude", "126.9780")
                .with(user(principal))
        ).andExpect(status().isBadRequest)
            .andDo(documentation("map-reverse-geocode-bad-request"))
    }

    @Test
    fun local_search_success_is_documented() {
        given(mapService.searchLocal(any(), any())).willReturn(mapOf("items" to emptyList<Any>()))

        mockMvc.perform(
            get("/api/maps/local-search")
                .param("query", "cafe")
                .param("display", "5")
                .with(user(principal))
        ).andExpect(status().isOk)
            .andDo(documentation("map-local-search-success"))
    }

    @Test
    fun local_search_invalid_display_fails() {
        mockMvc.perform(
            get("/api/maps/local-search")
                .param("query", "cafe")
                .param("display", "11")
                .with(user(principal))
        ).andExpect(status().isBadRequest)
            .andDo(documentation("map-local-search-bad-request"))
    }

    private fun documentation(identifier: String) =
        MockMvcRestDocumentationWrapper.document(
            identifier,
            ResourceSnippetParameters.builder().description("지도 API 성공·실패 케이스")
        )
}
