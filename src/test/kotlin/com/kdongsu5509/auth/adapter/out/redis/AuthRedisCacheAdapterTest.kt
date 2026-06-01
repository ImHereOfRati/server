package com.kdongsu5509.auth.adapter.out.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class AuthRedisCacheAdapterTest {

    @Mock
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    @Mock
    private lateinit var mapper: JsonMapper

    private lateinit var adapter: AuthRedisCacheAdapter

    @BeforeEach
    fun setUp() {
        adapter = AuthRedisCacheAdapter(redisTemplate, mapper)
    }

    @Test
    @DisplayName("save 호출 시 객체를 JSON 문자열로 변환하여 Redis에 저장한다")
    fun save() {
        whenever(mapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}")
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)

        adapter.save("testKey", mapOf("test" to "data"), Duration.ofMinutes(5))

        verify(mapper).writeValueAsString(any())
        verify(valueOperations).set(eq("testKey"), eq("{\"test\":\"data\"}"), eq(Duration.ofMinutes(5)))
    }

    data class DummyDto(val test: String)

    @Test
    @DisplayName("find 호출 시 JSON 문자열을 조회하여 객체로 변환한다")
    fun find() {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        whenever(valueOperations.get("testKey")).thenReturn("{\"test\":\"data\"}")
        whenever(mapper.readValue("{\"test\":\"data\"}", DummyDto::class.java)).thenReturn(DummyDto("data"))

        val result = adapter.find("testKey", DummyDto::class.java)

        assertThat(result?.test).isEqualTo("data")
    }

    @Test
    @DisplayName("delete 호출 시 키에 해당하는 데이터를 삭제한다")
    fun delete() {
        adapter.delete("testKey")
        verify(redisTemplate).delete("testKey")
    }
}
