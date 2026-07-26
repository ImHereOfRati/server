package com.kdongsu5509.maps

import org.springframework.stereotype.Service

@Service
class NaverMapProxyService(
    private val client: NaverMapProxyClient,
) {
    fun geocode(query: String): Map<String, Any?> =
        NaverMapResponseSanitizer.geocode(client.geocode(query.trim()))

    fun reverseGeocode(latitude: Double, longitude: Double): Map<String, Any?> =
        NaverMapResponseSanitizer.reverseGeocode(
            client.reverseGeocode(latitude, longitude),
        )

    fun searchLocal(query: String, display: Int): Map<String, Any?> =
        NaverMapResponseSanitizer.localSearch(
            client.searchLocal(query.trim(), display),
        )
}
