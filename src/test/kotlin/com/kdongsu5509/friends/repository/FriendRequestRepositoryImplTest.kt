package com.kdongsu5509.friends.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.jpa.FriendRequestJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.friends.repository.mapper.FriendRequestMapper
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestRepositoryImplTest {

    @Mock
    lateinit var entityManager: EntityManager

    @Mock
    lateinit var friendRequestMapper: FriendRequestMapper

    @Mock
    lateinit var springDataFriendRequestRepository: SpringDataFriendRequestRepository

    @InjectMocks
    lateinit var friendRequestRepositoryImpl: FriendRequestRepositoryImpl

    private fun createTestUser(id: UUID): User = User(
        id = id,
        email = "test@test.com",
        nickname = "test",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun createTestUserEntity(id: UUID): UserJpaEntity = UserJpaEntity(
        email = "test@test.com",
        nickname = "test",
        role = UserRole.NORMAL,
        provider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    ).apply { this.id = id }

    // 1. save
    @Nested
    @DisplayName("save 메서드는")
    inner class SaveTest {
        @Test
        @DisplayName("친구 요청을 성공적으로 저장한다")
        fun success() {
            // given
            val requesterId = UUID.randomUUID()
            val receiverId = UUID.randomUUID()

            val requester = createTestUser(requesterId)
            val receiver = createTestUser(receiverId)

            val requesterEntity = createTestUserEntity(requesterId)
            val receiverEntity = createTestUserEntity(receiverId)

            val domain = FriendRequest(
                id = UUID.randomUUID(),
                requester = requester,
                receiver = receiver,
                message = "안녕",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val entity = FriendRequestJpaEntity(requesterEntity, receiverEntity, "안녕")

            `when`(entityManager.getReference(UserJpaEntity::class.java, requesterId)).thenReturn(requesterEntity)
            `when`(entityManager.getReference(UserJpaEntity::class.java, receiverId)).thenReturn(receiverEntity)
            `when`(springDataFriendRequestRepository.save(any())).thenReturn(entity)
            `when`(friendRequestMapper.toDomain(entity)).thenReturn(domain)

            // when
            val result = friendRequestRepositoryImpl.save(domain)

            // then
            assertThat(result).isEqualTo(domain)
            verify(springDataFriendRequestRepository).save(any())
        }
    }

    // 2. findAll
    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 친구 요청 슬라이스를 조회한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val entity = FriendRequestJpaEntity(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "msg"
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = FriendRequest(
                id = UUID.randomUUID(),
                requester = createTestUser(UUID.randomUUID()),
                receiver = createTestUser(UUID.randomUUID()),
                message = "msg",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendRequestRepository.findAll(pageable)).thenReturn(slice)
            `when`(friendRequestMapper.toDomain(entity)).thenReturn(domain)

            val result = friendRequestRepositoryImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    // 3. findAllByReceiverEmail
    @Nested
    @DisplayName("findAllByReceiverEmail 메서드는")
    inner class FindAllByReceiverEmailTest {
        @Test
        @DisplayName("수신자 이메일로 친구 요청 목록을 조회한다")
        fun success() {
            // given
            val email = "receiver@test.com"
            val pageable = PageRequest.of(0, 10)
            val entity = FriendRequestJpaEntity(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "msg"
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = FriendRequest(
                id = UUID.randomUUID(),
                requester = createTestUser(UUID.randomUUID()),
                receiver = createTestUser(UUID.randomUUID()),
                message = "msg",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendRequestRepository.findAllByReceiverEmail(email, pageable)).thenReturn(slice)
            `when`(friendRequestMapper.toDomain(entity)).thenReturn(domain)

            // when
            val result = friendRequestRepositoryImpl.findAllByReceiverEmail(email, pageable)

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    // 4. findAllByRequesterEmail
    @Nested
    @DisplayName("findAllByRequesterEmail 메서드는")
    inner class FindAllByRequesterEmailTest {
        @Test
        @DisplayName("요청자 이메일로 친구 요청 목록을 조회한다")
        fun success() {
            val email = "requester@test.com"
            val pageable = PageRequest.of(0, 10)
            val entity = FriendRequestJpaEntity(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "msg"
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = FriendRequest(
                id = UUID.randomUUID(),
                requester = createTestUser(UUID.randomUUID()),
                receiver = createTestUser(UUID.randomUUID()),
                message = "msg",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendRequestRepository.findAllByRequesterEmail(email, pageable)).thenReturn(slice)
            `when`(friendRequestMapper.toDomain(entity)).thenReturn(domain)

            val result = friendRequestRepositoryImpl.findAllByRequesterEmail(email, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    // 5. findById
    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdTest {
        @Test
        @DisplayName("ID로 친구 요청을 단건 조회한다")
        fun success() {
            val id = UUID.randomUUID()
            val entity = FriendRequestJpaEntity(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "msg"
            ).apply { ReflectionTestUtils.setField(this, "id", id) }

            val domain = FriendRequest(
                id = id,
                requester = createTestUser(UUID.randomUUID()),
                receiver = createTestUser(UUID.randomUUID()),
                message = "msg",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendRequestRepository.findById(id)).thenReturn(Optional.of(entity))
            `when`(friendRequestMapper.toDomain(entity)).thenReturn(domain)

            val result = friendRequestRepositoryImpl.findById(id)

            assertThat(result).isEqualTo(domain)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 null을 반환한다")
        fun null_when_not_found() {
            val id = UUID.randomUUID()
            `when`(springDataFriendRequestRepository.findById(id)).thenReturn(Optional.empty())

            val result = friendRequestRepositoryImpl.findById(id)

            assertThat(result).isNull()
        }
    }

    // 6. deleteById
    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("ID로 친구 요청을 삭제한다")
        fun success() {
            val id = UUID.randomUUID()

            // when
            friendRequestRepositoryImpl.deleteById(id)

            // then
            verify(springDataFriendRequestRepository).deleteById(id)
        }
    }

    // 7. deleteBetween
    @Nested
    @DisplayName("deleteBetween 메서드는")
    inner class DeleteBetweenTest {
        @Test
        @DisplayName("두 사용자 간의 양방향 친구 요청을 모두 삭제한다")
        fun success() {
            val u1Id = UUID.randomUUID()
            val u2Id = UUID.randomUUID()

            val u1Entity = createTestUserEntity(u1Id)
            val u2Entity = createTestUserEntity(u2Id)

            `when`(entityManager.getReference(UserJpaEntity::class.java, u1Id)).thenReturn(u1Entity)
            `when`(entityManager.getReference(UserJpaEntity::class.java, u2Id)).thenReturn(u2Entity)

            // when
            friendRequestRepositoryImpl.deleteBetween(u1Id, u2Id)

            // then
            verify(springDataFriendRequestRepository).deleteByRequesterAndReceiver(u1Entity, u2Entity)
            verify(springDataFriendRequestRepository).deleteByRequesterAndReceiver(u2Entity, u1Entity)
        }
    }

    //8. existByRequesterEmailAndReceiverId
    @Test
    @DisplayName("기존의 친구 요청이 존재하는 지 잘 확인한다")
    fun existsByRequesterIdAndReceiverId_success() {
        val u1Id = UUID.randomUUID()
        val u2Id = UUID.randomUUID()

        val u1Entity = createTestUserEntity(u1Id)

        // when
        friendRequestRepositoryImpl.existsByRequesterIdAndReceiverId(u1Entity.id!!, u2Id)

        // then
        verify(springDataFriendRequestRepository).existsByRequesterIdAndReceiverId(u1Entity.id!!, u2Id)
    }
}
