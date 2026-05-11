package com.kdongsu5509.user.adapter.out.persistence.terms

import com.common.testUtil.TestRedisContainer
import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.config.RedisConfig
import com.kdongsu5509.user.adapter.out.persistence.terms.adapter.TermsDefinitionCommandPersistenceAdapter
import com.kdongsu5509.user.adapter.out.persistence.terms.adapter.TermsDefinitionQueryPersistenceAdapter
import com.kdongsu5509.user.adapter.out.persistence.terms.adapter.TermsVersionCommandPersistenceAdapter
import com.kdongsu5509.user.adapter.out.persistence.terms.adapter.TermsVersionQueryPersistenceAdapter
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermDefinitionMapper
import com.kdongsu5509.user.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@EnableCaching
@ActiveProfiles("test")
@Import(
    TermsDefinitionQueryPersistenceAdapter::class,
    TermsDefinitionCommandPersistenceAdapter::class,
    TermsVersionQueryPersistenceAdapter::class,
    TermsVersionCommandPersistenceAdapter::class,
    TermDefinitionMapper::class,
    TermVersionMapper::class,
    RedisConfig::class,
    QueryDslConfig::class
)
class TermsCacheTest : TestRedisContainer() {

    @Autowired
    private lateinit var termsDefinitionQueryPersistenceAdapter: TermsDefinitionQueryPersistenceAdapter

    @Autowired
    private lateinit var termsDefinitionCommandPersistenceAdapter: TermsDefinitionCommandPersistenceAdapter

    @Autowired
    private lateinit var termsVersionQueryPersistenceAdapter: TermsVersionQueryPersistenceAdapter

    @Autowired
    private lateinit var termsVersionCommandPersistenceAdapter: TermsVersionCommandPersistenceAdapter

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun clearCache() {
        cacheManager.getCache("terms")?.clear()
        cacheManager.getCache("term-versions")?.clear()
    }

    @Test
    @DisplayName("약관 목록 조회 시 결과가 캐싱된다")
    fun loadAllTerms_cache_success() {
        // given
        termsDefinitionCommandPersistenceAdapter.saveTermDefinition("Service Term", TermsTypes.SERVICE, true)

        // when
        val firstCall = termsDefinitionQueryPersistenceAdapter.loadAllTerms()
        val secondCall = termsDefinitionQueryPersistenceAdapter.loadAllTerms()

        // then
        assertThat(firstCall).hasSize(1)
        assertThat(secondCall).hasSize(1)
    }

    @Test
    @DisplayName("약관 정의 저장 시 기존 캐시가 삭제된다")
    fun saveTermDefinition_evict_cache_success() {
        // given
        termsDefinitionCommandPersistenceAdapter.saveTermDefinition("Service Term", TermsTypes.SERVICE, true)
        termsDefinitionQueryPersistenceAdapter.loadAllTerms() // Cache populated

        // when
        termsDefinitionCommandPersistenceAdapter.saveTermDefinition("Privacy Term", TermsTypes.PRIVACY, true)
        val result = termsDefinitionQueryPersistenceAdapter.loadAllTerms()

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    @DisplayName("특정 활성 약관 버전 조회 시 결과가 캐싱된다")
    fun loadSpecificActiveTermVersion_cache_success() {
        // given
        termsDefinitionCommandPersistenceAdapter.saveTermDefinition("Service Term", TermsTypes.SERVICE, true)
        val terms = termsDefinitionQueryPersistenceAdapter.loadAllTerms()
        val termId = terms[0].id

        termsVersionCommandPersistenceAdapter.saveTermVersion(termId, "v1.0", "Content", LocalDateTime.now())

        // when
        val firstCall = termsVersionQueryPersistenceAdapter.loadSpecificActiveTermVersion(termId)
        val secondCall = termsVersionQueryPersistenceAdapter.loadSpecificActiveTermVersion(termId)

        // then
        assertThat(firstCall.version).isEqualTo("v1.0")
        assertThat(secondCall.version).isEqualTo("v1.0")
    }

    @Test
    @DisplayName("약관 버전 저장 시 기존 캐시가 삭제된다")
    fun saveTermVersion_evict_cache_success() {
        // given
        termsDefinitionCommandPersistenceAdapter.saveTermDefinition("Service Term", TermsTypes.SERVICE, true)
        val terms = termsDefinitionQueryPersistenceAdapter.loadAllTerms()
        val termId = terms[0].id

        termsVersionCommandPersistenceAdapter.saveTermVersion(termId, "v1.0", "Content v1", LocalDateTime.now())
        termsVersionQueryPersistenceAdapter.loadSpecificActiveTermVersion(termId) // Cache populated

        // when
        termsVersionCommandPersistenceAdapter.saveTermVersion(termId, "v2.0", "Content v2", LocalDateTime.now())
        val result = termsVersionQueryPersistenceAdapter.loadSpecificActiveTermVersion(termId)

        // then
        assertThat(result.version).isEqualTo("v2.0")
    }
}
