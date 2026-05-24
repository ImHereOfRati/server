package com.kdongsu5509.friends.repository.mapper

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.jpa.FriendRequestJpaEntity
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserMapper
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FriendRequestMapperTest {

    @Mock
    lateinit var userMapper: UserMapper

    @InjectMocks
    lateinit var mapper: FriendRequestMapper

    @Test
    @DisplayName("FriendRequest 엔티티를 도메인 객체로 성공적으로 변환한다")
    fun toDomain_success() {
        // given
        val requesterId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()

        val requesterEntity = UserJpaEntity(
            "requester@test.com", "requester", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        ReflectionTestUtils.setField(requesterEntity, "id", requesterId)

        val receiverEntity = UserJpaEntity(
            "receiver@test.com", "receiver", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        ReflectionTestUtils.setField(receiverEntity, "id", receiverId)

        val requesterDomain = User(
            id = requesterId,
            email = "requester@test.com",
            nickname = "requester",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        val receiverDomain = User(
            id = receiverId,
            email = "receiver@test.com",
            nickname = "receiver",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        `when`(userMapper.toDomain(requesterEntity)).thenReturn(requesterDomain)
        `when`(userMapper.toDomain(receiverEntity)).thenReturn(receiverDomain)

        val entity = FriendRequestJpaEntity(
            requester = requesterEntity,
            receiver = receiverEntity,
            message = "안녕"
        )
        val id = UUID.randomUUID()
        ReflectionTestUtils.setField(entity, "id", id)

        val now = LocalDateTime.now()
        ReflectionTestUtils.setField(entity, "createdAt", now)
        ReflectionTestUtils.setField(entity, "updatedAt", now)

        // when
        val domain = mapper.toDomain(entity)

        // then
        assertThat(domain.id).isEqualTo(id)
        assertThat(domain.requester).isEqualTo(requesterDomain)
        assertThat(domain.receiver).isEqualTo(receiverDomain)
        assertThat(domain.message).isEqualTo("안녕")
        assertThat(domain.createdAt).isEqualTo(now)
        assertThat(domain.updatedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("FriendRequest 도메인 객체를 엔티티로 성공적으로 변환한다")
    fun toEntity_success() {
        // given
        val requesterId = UUID.randomUUID()
        val receiverId = UUID.randomUUID()

        val requesterDomain = User(
            id = requesterId,
            email = "requester@test.com",
            nickname = "requester",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        val receiverDomain = User(
            id = receiverId,
            email = "receiver@test.com",
            nickname = "receiver",
            role = UserRole.NORMAL,
            oauthProvider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )

        val requesterEntity = UserJpaEntity(
            "requester@test.com", "requester", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )
        val receiverEntity = UserJpaEntity(
            "receiver@test.com", "receiver", UserRole.NORMAL, OAuth2Provider.KAKAO, UserStatus.ACTIVE
        )

        `when`(userMapper.toEntity(requesterDomain)).thenReturn(requesterEntity)
        `when`(userMapper.toEntity(receiverDomain)).thenReturn(receiverEntity)

        val domain = FriendRequest(
            id = UUID.randomUUID(),
            requester = requesterDomain,
            receiver = receiverDomain,
            message = "만나서 반가워",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // when
        val entity = mapper.toEntity(domain)

        // then
        assertThat(entity.requester).isEqualTo(requesterEntity)
        assertThat(entity.receiver).isEqualTo(receiverEntity)
        assertThat(entity.message).isEqualTo("만나서 반가워")
    }
}
