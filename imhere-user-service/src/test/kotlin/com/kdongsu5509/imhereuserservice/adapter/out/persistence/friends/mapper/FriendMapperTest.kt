package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.mapper

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa.FriendshipJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.friend.FriendshipStatus
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class FriendMapperTest {

    companion object {
        const val testEmail = "dsko@kakao.com"
        const val testEmail2 = "dsko2@kakao.com"
        const val testNickname = "dsko"
        const val testNickname2 = "dsko2"
    }

    @Test
    @DisplayName("Friendship 엔티티를 도메인 객체로 잘 변환한다")
    fun mapping_success() {
        //given
        val testUserJpaEntity = UserJpaEntity(
            testEmail,
            testNickname,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ).apply {
            this.id = UUID.randomUUID()
        }
        val testUserJpaEntity2 = UserJpaEntity(
            testEmail2,
            testNickname2,
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        ).apply {
            this.id = UUID.randomUUID()
        }
        val testFriendshipJpaEntity = FriendshipJpaEntity(
            testUserJpaEntity,
            testUserJpaEntity2,
            FriendshipStatus.PENDING
        ).apply {
            this.id = UUID.randomUUID()
        }

        //when
        val result = FriendMapper().mapToDomainEntity(testFriendshipJpaEntity, "dsko@kakao.com")

        //then
        Assertions.assertEquals(testEmail2, result.opponentEmail)
        Assertions.assertEquals(testNickname2, result.opponentNickname)
    }

}