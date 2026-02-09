package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.mapper

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class FriendRequestMapperTest {

    private val mapper: FriendRequestMapper = FriendRequestMapper()

    @Test
    @DisplayName("entity를 domain 객체로 잘 변환한다")
    fun mapToDomainEntity_success() {
        //given
        val meEntity = UserJpaEntity(
            email = "ds.ko@kakao.com",
            nickname = "고동수",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        meEntity.id = UUID.randomUUID()

        val receiverEntity = UserJpaEntity(
            email = "receiver@kakao.com",
            nickname = "수신자",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        receiverEntity.id = UUID.randomUUID()

        val testFriendRequestJpaEntity = FriendRequestJpaEntity(
            meEntity, receiverEntity, "테스트용 메시지"
        )
        testFriendRequestJpaEntity.id = UUID.randomUUID()

        //when
        val result = mapper.mapToDomainEntity(testFriendRequestJpaEntity)

        //then
        Assertions.assertThat(result.friendRequestId).isEqualTo(testFriendRequestJpaEntity.id)
        Assertions.assertThat(result.message).isEqualTo(testFriendRequestJpaEntity.message)

        Assertions.assertThat(result.requester.id).isEqualTo(testFriendRequestJpaEntity.requester.id)
        Assertions.assertThat(result.requester.nickname).isEqualTo(testFriendRequestJpaEntity.requester.nickname)
        Assertions.assertThat(result.requester.email).isEqualTo(testFriendRequestJpaEntity.requester.email)

        Assertions.assertThat(result.receiver.id).isEqualTo(testFriendRequestJpaEntity.receiver.id)
        Assertions.assertThat(result.receiver.nickname).isEqualTo(testFriendRequestJpaEntity.receiver.nickname)
        Assertions.assertThat(result.receiver.email).isEqualTo(testFriendRequestJpaEntity.receiver.email)
    }

}