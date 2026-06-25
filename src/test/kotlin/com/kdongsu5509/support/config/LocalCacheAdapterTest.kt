package com.kdongsu5509.support.config

import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.auth.adapter.out.cache.LocalCacheAdapter
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class LocalCacheAdapterTest {

    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"))
    private val mapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
    private val adapter = LocalCacheAdapter(Caffeine.newBuilder(), mapper, clock)

    @Test
    @DisplayName("save 후 find 하면 객체를 복원한다")
    fun saveAndFind() {
        val original = OIDCPublicKeyResponse(
            keys = listOf(
                OIDCPublicKey(kid = "key1", kty = "kty", alg = "alg", use = "use", n = "n", e = "e")
            )
        )

        adapter.save("kakaoOidcKeys::kakaoPublicKeySet", original, Duration.ofHours(8))

        val cached = adapter.find("kakaoOidcKeys::kakaoPublicKeySet", OIDCPublicKeyResponse::class.java)

        assertThat(cached).isNotNull
        assertThat(cached!!.keys).hasSize(1)
        assertThat(cached.keys[0].kid).isEqualTo("key1")
    }

    @Test
    @DisplayName("만료 시간이 지나면 find 결과가 null 이다")
    fun expires() {
        adapter.save("refresh:test@example.com", "refresh-token", Duration.ofMinutes(5))

        clock.advanceBy(Duration.ofMinutes(6))

        assertThat(adapter.find("refresh:test@example.com", String::class.java)).isNull()
    }

    @Test
    @DisplayName("delete 후에는 조회되지 않는다")
    fun delete() {
        adapter.save("refresh:test@example.com", "refresh-token", Duration.ofMinutes(5))

        adapter.delete("refresh:test@example.com")

        assertThat(adapter.find("refresh:test@example.com", String::class.java)).isNull()
    }

    private class MutableClock(
        private var currentInstant: Instant,
        private val zoneId: ZoneId
    ) : Clock() {
        override fun getZone(): ZoneId = zoneId

        override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)

        override fun instant(): Instant = currentInstant

        fun advanceBy(duration: Duration) {
            currentInstant = currentInstant.plus(duration)
        }
    }
}
