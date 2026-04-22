package com.kdongsu5509.user.adapter.out.persistence.user.jpa

import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.util.*

@ActiveProfiles("test")
@DataJpaTest
@Import(SpringQueryDSLUserRepositoryTest.TestConfig::class)
class SpringQueryDSLUserRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringQueryDSLUserRepository
) {
    companion object {
        private const val TEST_OWNER_EMAIL = "owner@owner.com"
        private const val TEST_DOMAIN = "kakao.com"
        private const val DEFAULT_NICKNAME_PREFIX = "테스터"

        fun email(idx: Any) = "test$idx@$TEST_DOMAIN"
        fun nickname(idx: Any) = "$DEFAULT_NICKNAME_PREFIX$idx"
    }

    @TestConfiguration
    class TestConfig(private val em: EntityManager) {
        @Bean
        fun jpaQueryFactory() = JPAQueryFactory(em)

        @Bean
        fun springQueryDSLUserRepository(jpaQueryFactory: JPAQueryFactory) =
            SpringQueryDSLUserRepository(jpaQueryFactory)
    }

    @BeforeEach
    fun setUp() {
        createTestOwner()
        val activeUsers = (1..3).map { createTestUser(it, UserStatus.ACTIVE) }
        val pendingUsers = (4..5).map { createTestUser(it, UserStatus.PENDING) }
        saveAll(activeUsers + pendingUsers)
    }

    /**
     * findUserByEmail
     */
    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3, 4, 5])
    @DisplayName("상태에 상관없이 email로 사용자를 조회한다")
    fun findUserByEmail_success(idx: Int) {
        val targetEmail = email(idx)
        val result = userRepository.findUserByEmail(targetEmail)

        Assertions.assertTrue(result.isPresent)
        Assertions.assertEquals(targetEmail, result.get().email)
    }

    /**
     * findActiveUserByEmail
     */
    @ParameterizedTest
    @ValueSource(ints = [1, 2, 3])
    @DisplayName("ACTIVE 상태인 사용자는 이메일로 검색된다")
    fun findActiveUserByEmail_active(idx: Int) {
        Assertions.assertTrue(userRepository.findActiveUserByEmail(email(idx)).isPresent)
    }

    @ParameterizedTest
    @ValueSource(ints = [4, 5, 999])
    @DisplayName("ACTIVE가 아니거나 존재하지 않으면 검색되지 않는다")
    fun findActiveUserByEmail_fail(idx: Int) {
        Assertions.assertTrue(userRepository.findActiveUserByEmail(email(idx)).isEmpty)
    }

    /**
     * searchNewFriendCandidates
     */
    @ParameterizedTest
    @DisplayName("키워드(닉네임/이메일)로 활성 사용자를 정확히 찾는다")
    @ValueSource(strings = ["테스터1", "test2@kakao.com"])
    fun searchNewFriendCandidates_success(testKeyword: String) {
        val result = userRepository.searchNewFriendCandidates(TEST_OWNER_EMAIL, testKeyword)
        Assertions.assertEquals(1, result.size)
    }

    @Test
    @DisplayName("중복된 닉네임이 있는 경우 모두 조회된다")
    fun searchNewFriendCandidates_duplication() {
        val dupNickname = "중복닉네임"
        saveAll(
            listOf(
                createTestUser(100, UserStatus.ACTIVE, dupNickname),
                createTestUser(101, UserStatus.ACTIVE, dupNickname)
            )
        )

        val result = userRepository.searchNewFriendCandidates(TEST_OWNER_EMAIL, dupNickname)
        Assertions.assertEquals(2, result.size)
    }

    @Test
    @DisplayName("키워드(이메일/닉네임)가 비어 있거나 일치하는게 없으면 빈 리스트가 반환된다")
    fun findActiveUsersByEmailOrNickname_empty_or_zero_match() {
        Assertions.assertTrue(userRepository.searchNewFriendCandidates(TEST_OWNER_EMAIL, "").isEmpty())
        Assertions.assertTrue(userRepository.searchNewFriendCandidates(TEST_OWNER_EMAIL, "존재하지않음").isEmpty())
    }

    /**
     * findUsersByEmails: 이메일 기반 다중 조회
     */
    @Test
    @DisplayName("활성 유저와 비활성 유저 혼합 조회 시 활성 유저만 결과에 포함된다")
    fun findActiveUsersByEmails_logic_check() {
        val (activeIdx, pendingIdx) = 300 to 301
        saveAll(listOf(createTestUser(activeIdx, UserStatus.ACTIVE), createTestUser(pendingIdx, UserStatus.PENDING)))

        val testResult = userRepository.findActiveUsersByEmails(email(activeIdx), email(pendingIdx))

        Assertions.assertEquals(1, testResult.size)
        Assertions.assertEquals(email(activeIdx), testResult[0].email)
    }

    /**
     * findUsersByEmailAndId : 이메일과 ID 를 바탕으로 혼합 조회
     * 해당 테스트의 경우 ID 주입이 어려워 UUID.randomUUID() 을 통해 존재하지 않는 유저를 조회합니다.
     */
    @Test
    @DisplayName("Email과 ID로도 타겟 User를 잘 조회한다 - 해당하는 경우에만.")
    fun findActiveUsersByEmailAndId_logic_check() {
        val idx1 = 300
        saveAll(
            listOf(
                createTestUser(idx1, UserStatus.ACTIVE)
            )
        )

        val testResult = userRepository.findActiveUsersByEmailAndId(
            email(idx1), UUID.randomUUID()
        )

        Assertions.assertEquals(1, testResult.size)
        Assertions.assertEquals(email(idx1), testResult[0].email)
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
                nickname = "나",
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
