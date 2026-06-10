package com.kdongsu5509.support.config

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.CachePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class RedisCacheIntegrationTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var cachePort: CachePort

    @Test
    fun testOidcCacheSerialization() {
        val key = "kakaoOidcKeys::kakaoPublicKeySet"
        val original = OIDCPublicKeyResponse(
            keys = listOf(
                OIDCPublicKey(kid = "key1", kty = "kty", alg = "alg", use = "use", n = "n", e = "e")
            )
        )

        cachePort.save(key, original, Duration.ofDays(8))

        val cachedValue = cachePort.find(key, OIDCPublicKeyResponse::class.java)
        assertThat(cachedValue).isNotNull
        assertThat(cachedValue).isInstanceOf(OIDCPublicKeyResponse::class.java)

        val deserialized = cachedValue as OIDCPublicKeyResponse
        assertThat(deserialized.keys).hasSize(1)
        assertThat(deserialized.keys[0].kid).isEqualTo("key1")
    }
}
