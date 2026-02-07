package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa

import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@DataJpaTest
@Import(SpringQueryDSLUserRepositoryTest.TestConfig::class)
class SpringQueryDSLUserRepositoryTest @Autowired constructor(
    private val em: EntityManager,
    private val userRepository: SpringQueryDSLUserRepository
) {

    @TestConfiguration
    class TestConfig(private val em: EntityManager) {
        @Bean
        fun jpaQueryFactory(): JPAQueryFactory = JPAQueryFactory(em)

        @Bean
        fun springQueryDSLUserRepository(jpaQueryFactory: JPAQueryFactory) =
            SpringQueryDSLUserRepository(jpaQueryFactory)
    }

    @BeforeEach
    @DisplayName("테스트 USER 주입")
    fun setUpUser() {
        val testUser1 =
            UserJpaEntity("test1@kakao.com", "테스터1", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)
        val testUser2 =
            UserJpaEntity("test2@kakao.com", "테스터2", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)
        val testUser3 =
            UserJpaEntity("test3@kakao.com", "테스터3", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)

        val testUser4 =
            UserJpaEntity("test4@kakao.com", "테스터4", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.PENDING)
        val testUser5 =
            UserJpaEntity("test5@kakao.com", "테스터5", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.PENDING)

        val testUsers = listOf(testUser1, testUser2, testUser3, testUser4, testUser5)
        saveTestUsers(testUsers)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test999@kakao.com", "test1000@kakao.com"])
    @DisplayName("없는 사용자들을 검색하면 조회되지 않는다")
    fun findActiveUserByEmail_not_exist_user(testEmail: String) {
        //when
        val queryResult = userRepository.findActiveUserByEmail(testEmail)
        //then
        Assertions.assertEquals(true, queryResult.isEmpty)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test4@kakao.com", "test5@kakao.com"])
    @DisplayName("비활성화 된 사용자들을 검색하면 조회되지 않는다")
    fun findActiveUserByEmail_pending_user(testEmail: String) {
        //when
        val queryResult = userRepository.findActiveUserByEmail(testEmail)
        //then
        Assertions.assertEquals(true, queryResult.isEmpty)
    }

    @ParameterizedTest
    @ValueSource(strings = ["테스터1", "테스터2", "테스터3", "test1@kakao.com", "test2@kakao.com", "test3@kakao.com"])
    @DisplayName("nickname혹은 email로 잘 찾는다")
    fun findWithNickname(testKeyword: String) {
        //when
        val queryResult = userRepository.findUserByKeyword(testKeyword)

        //then
        val expectSize = 1
        val isMatchingWithNicknameOrEmail =
            testKeyword.equals(queryResult[0].nickname) || testKeyword.equals(queryResult[0].email)
        Assertions.assertEquals(expectSize, queryResult.size)
        Assertions.assertTrue(isMatchingWithNicknameOrEmail)
    }

    @ParameterizedTest
    @ValueSource(strings = ["unknown@kakao.com", "unknown2@kakao.com", "언노운", "몰라요"])
    @DisplayName("존재하지 않는 이메일 혹은 닉네임인 경우 빈 List를 반환한다")
    fun NotfindAnythingIfNotExistEmailOrNickname(testKeyword: String) {
        //when
        val queryResult = userRepository.findUserByKeyword(testKeyword)

        //then
        val expectSize = 0
        Assertions.assertEquals(expectSize, queryResult.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["테스터1", "테스터2", "테스터3"])
    @DisplayName("nickname혹은 email로 잘 찾는다")
    fun findWithDuplicatedNickname(testKeyword: String) {
        //given
        createAndSaveDuplicatedNicknameUsers()

        //when
        val queryResult = userRepository.findUserByKeyword(testKeyword)

        //then
        val expectSize = 2
        Assertions.assertEquals(expectSize, queryResult.size)
        Assertions.assertTrue(testKeyword.equals(queryResult[0].nickname))
    }

    private fun createAndSaveDuplicatedNicknameUsers() {
        val duplicatedNameUser1 =
            UserJpaEntity("test11@kakao.com", "테스터1", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)
        val duplicatedNameUser2 =
            UserJpaEntity("test12@kakao.com", "테스터2", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)
        val duplicatedNameUser3 =
            UserJpaEntity("test13@kakao.com", "테스터3", UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE)

        val duplicatedNicknameUsers = listOf(duplicatedNameUser1, duplicatedNameUser2, duplicatedNameUser3)
        saveTestUsers(duplicatedNicknameUsers)
    }

    private fun saveTestUsers(users: List<UserJpaEntity>) {
        for (user in users) {
            em.persist(user)
        }

        em.flush()
        em.clear()
    }
}