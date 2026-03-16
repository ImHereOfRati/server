package com.kdongsu5509.imhere.message.application.service

import com.kdongsu5509.imhere.TestFirebaseConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@Import(TestFirebaseConfig::class)
@SpringBootTest(
    properties = [
        "solapi.sender=01011112222",
        "solapi.apiKey=apikey",
        "solapi.apiSecret=apiSecret"
    ]
)
class SolapiPropertiesTest {

    @Autowired
    private lateinit var solapiProperties: SolapiProperties

    @Test
    @DisplayName("Solapi 설정 정보를 정확하게 가져온다")
    fun propertiesLoadTest() {
        assertThat(solapiProperties.sender).isEqualTo("01011112222")

        assertThat(solapiProperties.apiKey).isEqualTo("apikey")

        assertThat(solapiProperties.apiSecret).isEqualTo("apiSecret")
    }
}