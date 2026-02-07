package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.friend.FriendshipStatus
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional

@Transactional
@DataJpaTest
@Import(SpringQueryDSLFriendRepositoryTest.TestConfig::class)
class SpringQueryDSLFriendRepositoryTest @Autowired constructor(
    private val userRepository: SpringDataUserRepository,
    private val friendRepository: SpringDataFriendshipRepository,
    private val friendQueryDSLRepository: SpringQueryDSLFriendRepository
) {

    companion object {
        const val testEmailOne = "dsko@kakao.com"
        const val testEmailTwo = "dsko2@kakao.com"
        const val testNicknameOne = "고동수"
        const val testNicknameTwo = "dongsuKo"
    }

    @TestConfiguration
    class TestConfig(private val em: EntityManager) {
        @Bean
        fun jpaQueryFactory(): JPAQueryFactory = JPAQueryFactory(em)

        @Bean
        fun springQueryDSLFriendRepository(jpaQueryFactory: JPAQueryFactory) =
            SpringQueryDSLFriendRepository(jpaQueryFactory)
    }

    @BeforeEach
    fun settingUsers() {
        saveTwoUsers()
    }

    @Test
    fun findSentRequests() {
        //given
        saveFriendshipJpaEntity(FriendshipStatus.PENDING)

        //when
        val queryResult = friendQueryDSLRepository.findSentRequests(testEmailOne)

        //then
        checkValidation(queryResult)
    }

    @Test
    fun findReceivedRequests() {
        saveFriendshipJpaEntity(FriendshipStatus.PENDING)

        val queryResult = friendQueryDSLRepository.findReceivedRequests(testEmailTwo)

        //then
        checkValidation(queryResult)
    }

    @ParameterizedTest
    @ValueSource(strings = [testEmailOne, testEmailTwo])
    fun findAcceptedFriends(email: String) {
        saveFriendshipJpaEntity(FriendshipStatus.ACCEPTED)

        val queryResult = friendQueryDSLRepository.findAcceptedFriends(email)

        checkValidation(queryResult)
    }

    private fun saveTwoUsers() {
        val testEntityOne = UserJpaEntity(
            testEmailOne, testNicknameOne, UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE
        )
        val testEntityTwo = UserJpaEntity(
            testEmailTwo, testNicknameTwo, UserRole.NORMAL, OAuth2Provider.KAKAO, status = UserStatus.ACTIVE
        )
        userRepository.save(testEntityOne)
        userRepository.save(testEntityTwo)
    }

    private fun saveFriendshipJpaEntity(status: FriendshipStatus) {
        friendRepository.save(
            FriendshipJpaEntity(
                userRepository.findByEmail(testEmailOne),
                userRepository.findByEmail(testEmailTwo),
                status
            )
        )
    }

    private fun checkValidation(queryResult: List<FriendshipJpaEntity>) {
        Assertions.assertEquals(1, queryResult.size)
        Assertions.assertEquals(testNicknameOne, queryResult[0].requester!!.nickname)
        Assertions.assertEquals(testEmailOne, queryResult[0].requester!!.email)
        Assertions.assertEquals(testEmailTwo, queryResult[0].receiver!!.email)
        Assertions.assertEquals(testNicknameTwo, queryResult[0].receiver!!.nickname)
    }
}