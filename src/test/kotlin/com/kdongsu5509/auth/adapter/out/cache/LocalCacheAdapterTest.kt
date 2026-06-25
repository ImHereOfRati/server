package com.kdongsu5509.auth.adapter.out.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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

    private lateinit var adapter: LocalCacheAdapter
    private lateinit var clock: MutableClock

    data class DummyDto(val test: String)

    @BeforeEach
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"))
        val mapper = JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()
        adapter = LocalCacheAdapter(Caffeine.newBuilder(), mapper, clock)
    }

    @Test
    @DisplayName("save 하면 JSON 직렬화 값을 캐시에 저장한다")
    fun save() {
        adapter.save("testKey", DummyDto("data"), Duration.ofMinutes(5))

        val result = adapter.find("testKey", DummyDto::class.java)

        assertThat(result?.test).isEqualTo("data")
    }

    @Test
    @DisplayName("유효 시간이 지나면 조회되지 않는다")
    fun expires() {
        adapter.save("testKey", DummyDto("data"), Duration.ofMinutes(5))

        clock.advanceBy(Duration.ofMinutes(6))

        assertThat(adapter.find("testKey", DummyDto::class.java)).isNull()
    }

    @Test
    @DisplayName("delete 호출 시 키가 삭제된다")
    fun delete() {
        adapter.save("testKey", DummyDto("data"), Duration.ofMinutes(5))

        adapter.delete("testKey")

        assertThat(adapter.find("testKey", DummyDto::class.java)).isNull()
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
