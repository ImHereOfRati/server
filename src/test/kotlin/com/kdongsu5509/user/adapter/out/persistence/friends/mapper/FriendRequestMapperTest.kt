package com.kdongsu5509.user.adapter.out.persistence.friends.mapper

import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserRole
import com.kdongsu5509.user.domain.user.UserStatus
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class FriendRequestMapperTest {

    private val mapper: FriendRequestMapper = FriendRequestMapper()

    @Test
    @DisplayName("entity�?domain 객체�???변?�한??")
    fun mapToDomainEntity_success() {
        //given
        val meEntity = UserJpaEntity(
            email = "ds.ko@kakao.com",
            nickname = "고동??",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        meEntity.id = UUID.randomUUID()

        val receiverEntity = UserJpaEntity(
            email = "receiver@kakao.com",
            nickname = "?�신??",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        receiverEntity.id = UUID.randomUUID()

        val testFriendRequestJpaEntity = FriendRequestJpaEntity(
            meEntity, receiverEntity, "?�스?�용 메시지"
        )
        testFriendRequestJpaEntity.id = 1L

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
