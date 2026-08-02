package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.support.config.QueryDslConfig
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
@Import(SpringQueryDSLUserRepository::class, QueryDslConfig::class)
class SpringQueryDSLUserRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringQueryDSLUserRepository
) {

    companion object {
        private const val TEST_OWNER_EMAIL = "owner@owner.com"
        private const val TEST_DOMAIN = "kakao.com"
        private const val DEFAULT_NICKNAME_PREFIX = "테스트"

        fun email(idx: Any) = "test$idx@$TEST_DOMAIN"
        fun nickname(idx: Any) = "$DEFAULT_NICKNAME_PREFIX$idx"
    }

    @BeforeEach
    fun setUp() {
        createTestOwner()
        val activeUsers = (1..3).map { createTestUser(it, UserStatus.ACTIVE) }
        val pendingUsers = (4..5).map { createTestUser(it, UserStatus.PENDING) }
        saveAll(activeUsers + pendingUsers)
    }

    @ParameterizedTest
    @DisplayName("키워드(닉네임/이메일)가 활성 사용자와 일치하면 정확히 찾는다")
    @ValueSource(strings = ["테스트2", "test2@kakao.com"])
    fun findAllActiveByKeyword_success(testKeyword: String) {
        // when
        val result = userRepository.findAllActiveByKeyword(testKeyword, emptySet())

        // then
        assertThat(result.content).hasSize(1)
    }

    @Test
    @DisplayName("중복된 닉네임이 있는 경우 모두 조회한다")
    fun findAllActiveByKeyword_duplication() {
        // given
        val dupNickname = "중복닉네임"
        saveAll(
            listOf(
                createTestUser(100, UserStatus.ACTIVE, dupNickname),
                createTestUser(101, UserStatus.ACTIVE, dupNickname)
            )
        )

        // when
        val result = userRepository.findAllActiveByKeyword(
            dupNickname,
            emptySet(),
            PageRequest.of(0, 1)
        )

        // then
        assertThat(result.content).hasSize(1)
    }

    @Test
    @DisplayName("제외 목록에 있는 사용자는 키워드가 일치해도 결과에서 뺀다")
    fun findAllActiveByKeyword_excludes_given_ids() {
        // given
        val excludedNickname = "제외대상"
        val excluded = createTestUser(200, UserStatus.ACTIVE, excludedNickname)
        val kept = createTestUser(201, UserStatus.ACTIVE, excludedNickname)
        saveAll(listOf(excluded, kept))

        // when
        val result = userRepository.findAllActiveByKeyword(excludedNickname, setOf(excluded.id!!))

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].id).isEqualTo(kept.id)
    }

    @Test
    @DisplayName("키워드(이메일/닉네임)가 비어 있거나 일치하는게 없으면 빈 리스트를 반환한다")
    fun findAllActiveByKeyword_empty_or_zero_match() {
        // when & then
        assertThat(userRepository.findAllActiveByKeyword("", emptySet()).content).isEmpty()
        assertThat(userRepository.findAllActiveByKeyword("존재하지않음", emptySet()).content).isEmpty()
    }

    @Test
    @DisplayName("무한 스크롤(Slice) 조회 시 다음 페이지 존재 여부(hasNext)가 참(true)인 경우를 정확히 테스트한다")
    fun findAllActiveByKeyword_slice_hasNext_true() {
        // given
        val sliceNickname = "슬라이스닉네임"
        saveAll(
            listOf(
                createTestUser(500, UserStatus.ACTIVE, sliceNickname),
                createTestUser(501, UserStatus.ACTIVE, sliceNickname),
                createTestUser(502, UserStatus.ACTIVE, sliceNickname)
            )
        )
        val pageable = PageRequest.of(0, 2)

        // when
        val result = userRepository.findAllActiveByKeyword(
            keyword = sliceNickname,
            excludedUserIds = emptySet(),
            pageable = pageable
        )

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("무한 스크롤(Slice) 조회 시 다음 페이지 존재 여부(hasNext)가 거짓(false)인 경우를 정확히 테스트한다")
    fun findAllActiveByKeyword_slice_hasNext_false() {
        // given
        val sliceNickname = "슬라이스닉네임"
        saveAll(
            listOf(
                createTestUser(600, UserStatus.ACTIVE, sliceNickname),
                createTestUser(601, UserStatus.ACTIVE, sliceNickname),
                createTestUser(602, UserStatus.ACTIVE, sliceNickname)
            )
        )
        val pageable = PageRequest.of(0, 3)

        // when
        val result = userRepository.findAllActiveByKeyword(
            keyword = sliceNickname,
            excludedUserIds = emptySet(),
            pageable = pageable
        )

        // then
        assertThat(result.content).hasSize(3)
        assertThat(result.hasNext()).isFalse()
    }

    private fun createTestUser(idx: Int, status: UserStatus, customNickname: String? = null) = UserJpaEntity(
        email = email(idx),
        nickname = customNickname ?: nickname(idx),
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = status
    )

    private fun createTestOwner() {
        em.persist(
            UserJpaEntity(
                email = TEST_OWNER_EMAIL,
                nickname = "주인",
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO,
                status = UserStatus.ACTIVE
            )
        )

        em.flush()
        em.clear()
    }

    private fun saveAll(users: List<UserJpaEntity>) {
        users.forEach(em::persist)
        em.flush()
        em.clear()
    }
}
