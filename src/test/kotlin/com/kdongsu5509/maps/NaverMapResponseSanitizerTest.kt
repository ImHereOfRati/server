package com.kdongsu5509.maps

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NaverMapResponseSanitizerTest {
    @Test
    fun `geocode keeps only client fields`() {
        val result = NaverMapResponseSanitizer.geocode(
            mapOf(
                "addresses" to listOf(
                    mapOf(
                        "roadAddress" to "서울시",
                        "x" to "127.0",
                        "y" to "37.0",
                        "secret" to "must-not-leak",
                    ),
                ),
                "apiKey" to "must-not-leak",
            ),
        )

        assertThat(result.toString()).contains("서울시")
        assertThat(result.toString()).doesNotContain("secret", "apiKey")
    }

    @Test
    fun `local search strips unapproved upstream fields`() {
        val result = NaverMapResponseSanitizer.localSearch(
            mapOf(
                "items" to listOf(
                    mapOf(
                        "title" to "카페",
                        "roadAddress" to "서울시",
                        "unapproved" to "hidden",
                    ),
                ),
            ),
        )

        assertThat(result.toString()).contains("카페")
        assertThat(result.toString()).doesNotContain("unapproved", "hidden")
    }
}
