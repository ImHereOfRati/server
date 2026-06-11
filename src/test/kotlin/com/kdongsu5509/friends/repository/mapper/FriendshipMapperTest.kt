package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.jpa.FriendshipJpaEntity
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.UserMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendshipMapperTest {

    @Mock
    lateinit var userMapper: UserMapper

    @InjectMocks
    lateinit var friendshipMapper: FriendshipMapper

    @Test
    @DisplayName("FriendRelationshipsJpaEntity를 FriendRelationship 도메인 객체로 변환한다")
    fun toDomain_success() {
        // given
        val ownerId = UUID.randomUUID()
        val friendId = UUID.randomUUID()

        val ownerEntity =
            UserJpaEntity("owner@test.com", "owner", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE).apply {
                this.id = ownerId
            }
        val friendEntity =
            UserJpaEntity("friend@test.com", "friend", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE).apply {
                this.id = friendId
            }

        val ownerDomain =
            User(ownerId, "owner@test.com", "owner", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        val friendDomain =
            User(friendId, "friend@test.com", "friend", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)

        given(userMapper.toDomain(ownerEntity)).willReturn(ownerDomain)
        given(userMapper.toDomain(friendEntity)).willReturn(friendDomain)

        val entity = FriendshipJpaEntity.create(ownerEntity, friendEntity, "friendAlias")
        val relationshipId = UUID.randomUUID()
        ReflectionTestUtils.setField(entity, "id", relationshipId)

        val testTime = LocalDateTime.of(2026, 5, 21, 12, 0)
        entity.createdAt = testTime

        // when
        val result = friendshipMapper.toDomain(entity)

        // then
        assertThat(result.id).isEqualTo(relationshipId)
        assertThat(result.friend.email).isEqualTo(friendEntity.email)
        assertThat(result.friendAlias).isEqualTo("friendAlias")
        assertThat(result.createdAt).isEqualTo(testTime)
    }

    @Test
    @DisplayName("FriendRelationship 도메인 객체를 FriendRelationshipsJpaEntity로 변환한다")
    fun toEntity_success() {
        // given
        val relationshipId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val friendId = UUID.randomUUID()

        val ownerDomain =
            User(ownerId, "owner@test.com", "owner", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)
        val friendDomain =
            User(friendId, "friend@test.com", "friend", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE)

        val testTime = LocalDateTime.of(2026, 5, 21, 12, 0)
        val domain = Friendship(
            id = relationshipId,
            owner = ownerDomain,
            friend = friendDomain,
            friendAlias = "friendAlias",
            createdAt = testTime
        )

        val ownerEntity = UserJpaEntity(
            "owner@test.com",
            "owner",
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            UserStatus.ACTIVE
        ).apply { this.id = ownerId }
        val friendEntity = UserJpaEntity(
            "friend@test.com",
            "friend",
            UserRole.NORMAL,
            OAuth2Provider.KAKAO,
            UserStatus.ACTIVE
        ).apply { this.id = friendId }

        // 도메인 -> 엔티티 변환 시 내부 유저 매퍼 모킹
        given(userMapper.toEntity(ownerDomain)).willReturn(ownerEntity)
        given(userMapper.toEntity(friendDomain)).willReturn(friendEntity)

        // when
        val result = friendshipMapper.toEntity(domain)

        // then
        assertThat(result.id).isNull()
        assertThat(result.ownerUser.id).isEqualTo(ownerId)
        assertThat(result.friendUser.id).isEqualTo(friendId)
        assertThat(result.friendAlias).isEqualTo("friendAlias")
    }
}
