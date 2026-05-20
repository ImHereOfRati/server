package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
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
import java.util.*

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
    @ValueSource(ints = [1, 2, 3, 4, 5])
    @DisplayName("상태와 관계없이 email을 사용하여 조회한다")
    fun findUserByEmail_success(idx: Int) {
        // given
        val targetEmail = email(idx)

        // when
        val result = userRepository.findUserByEmail(targetEmail)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.email).isEqualTo(targetEmail)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    @DisplayName("ACTIVE 상태의 사용자는 이메일로 검색된다")
    fun findActiveUserByEmail_success(idx: Int) {
        // when
        val result = userRepository.findActiveUserByEmail(email(idx))

        // then
        assertThat(result).isNotNull
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 5, 999])
    @DisplayName("ACTIVE가 아니거나 존재하지 않으면 검색되지 않는다")
    fun findActiveUserByEmail_fail(idx: Int) {
        // when
        val result = userRepository.findActiveUserByEmail(email(idx))

        // then
        assertThat(result).isNull()
    }

    @ParameterizedTest
    @DisplayName("키워드(닉네임/이메일)가 활성 사용자와 일치하면 정확히 찾는다")
    @ValueSource(strings = ["테스트2", "test2@kakao.com"])
    fun findAllActiveByEmailAndKeyword_success(testKeyword: String) {
        // when
        val result = userRepository.findAllActiveByEmailAndKeyword(TEST_OWNER_EMAIL, testKeyword)

        // then
        assertThat(result.content).hasSize(1)
    }

    @Test
    @DisplayName("중복된 닉네임이 있는 경우 모두 조회한다")
    fun findAllActiveByEmailAndKeyword_duplication() {
        // given
        val dupNickname = "중복닉네임"
        saveAll(
            listOf(
                createTestUser(100, UserStatus.ACTIVE, dupNickname),
                createTestUser(101, UserStatus.ACTIVE, dupNickname)
            )
        )

        // when
        val result = userRepository.findAllActiveByEmailAndKeyword(
            TEST_OWNER_EMAIL,
            dupNickname,
            PageRequest.of(0, 1)
        )

        // then
        assertThat(result.content).hasSize(1)
    }

    @Test
    @DisplayName("키워드(이메일/닉네임)가 비어 있거나 일치하는게 없으면 빈 리스트를 반환한다")
    fun findAllActiveByEmailAndKeyword_empty_or_zero_match() {
        // when & then
        assertThat(userRepository.findAllActiveByEmailAndKeyword(TEST_OWNER_EMAIL, "").content).isEmpty()
        assertThat(userRepository.findAllActiveByEmailAndKeyword(TEST_OWNER_EMAIL, "존재하지않음").content).isEmpty()
    }

    @Test
    @DisplayName("활성 사용자와 비활성 사용자 혼합 조회 시 활성 사용자만 결과에 포함한다")
    fun findActiveUsersByEmails_success() {
        // given
        val (activeIdx, pendingIdx) = 300 to 301
        saveAll(listOf(createTestUser(activeIdx, UserStatus.ACTIVE), createTestUser(pendingIdx, UserStatus.PENDING)))

        // when
        val testResult = userRepository.findActiveUsersByEmails(email(activeIdx), email(pendingIdx))

        // then
        assertThat(testResult).hasSize(1)
        assertThat(testResult[0].email).isEqualTo(email(activeIdx))
    }

    @Test
    @DisplayName("Email과 ID로도 활성 User를 조회한다 - 해당되는 경우에만.")
    fun findActiveUsersByEmailAndId_success() {
        // given
        val idx1 = 300
        saveAll(
            listOf(
                createTestUser(idx1, UserStatus.ACTIVE)
            )
        )

        // when
        val testResult = userRepository.findActiveUsersByEmailAndId(
            email(idx1), UUID.randomUUID()
        )

        // then
        assertThat(testResult).hasSize(1)
        assertThat(testResult[0].email).isEqualTo(email(idx1))
    }

    @Test
    @DisplayName("무한 스크롤(Slice) 조회 시 다음 페이지 존재 여부(hasNext)가 참(true)인 경우를 정확히 테스트한다")
    fun findAllActiveByEmailAndKeyword_slice_hasNext_true() {
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
        val result = userRepository.findAllActiveByEmailAndKeyword(
            userEmail = TEST_OWNER_EMAIL,
            keyword = sliceNickname,
            pageable = pageable
        )

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    @DisplayName("무한 스크롤(Slice) 조회 시 다음 페이지 존재 여부(hasNext)가 거짓(false)인 경우를 정확히 테스트한다")
    fun findAllActiveByEmailAndKeyword_slice_hasNext_false() {
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
        val result = userRepository.findAllActiveByEmailAndKeyword(
            userEmail = TEST_OWNER_EMAIL,
            keyword = sliceNickname,
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
