package com.kdongsu5509.friends.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.jpa.FriendshipJpaEntity
import com.kdongsu5509.friends.repository.jpa.SpringDataFriendshipRepository
import com.kdongsu5509.friends.repository.mapper.FriendshipMapper
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
class FriendshipRepositoryImplTest {

    @Mock
    lateinit var entityManager: EntityManager

    @Mock
    lateinit var friendshipMapper: FriendshipMapper

    @Mock
    lateinit var springDataFriendshipRepository: SpringDataFriendshipRepository

    @InjectMocks
    lateinit var friendshipRepositoryImpl: FriendshipRepositoryImpl

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

    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdTest {
        @Test
        @DisplayName("ID로 친구 관계를 단건 조회한다")
        fun success() {
            val id = UUID.randomUUID()
            val entity = FriendshipJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "alias"
            ).apply { ReflectionTestUtils.setField(this, "id", id) }

            val domain = Friendship(
                id = id,
                owner = createTestUser(UUID.randomUUID()),
                friend = createTestUser(UUID.randomUUID()),
                friendAlias = "alias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendshipRepository.findById(id)).thenReturn(Optional.of(entity))
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.findById(id)

            assertThat(result).isEqualTo(domain)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 null을 반환한다")
        fun null_when_not_found() {
            val id = UUID.randomUUID()

            `when`(springDataFriendshipRepository.findById(id)).thenReturn(Optional.empty())

            val result = friendshipRepositoryImpl.findById(id)

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByOwnerEmail 메서드는")
    inner class FindByOwnerEmailTest {
        @Test
        @DisplayName("소유자 이메일로 친구 관계 슬라이스를 조회한다")
        fun success() {
            val email = "owner@test.com"
            val pageable = PageRequest.of(0, 10)

            val entity = FriendshipJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "alias"
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = Friendship(
                id = UUID.randomUUID(),
                owner = createTestUser(UUID.randomUUID()),
                friend = createTestUser(UUID.randomUUID()),
                friendAlias = "alias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendshipRepository.findByOwnerUserEmail(email, pageable)).thenReturn(slice)
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.findByOwnerEmail(email, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 친구 관계 슬라이스를 조회한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)

            val entity = FriendshipJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "alias"
            )
            val slice = PageImpl(listOf(entity), pageable, 1L)
            val domain = Friendship(
                id = UUID.randomUUID(),
                owner = createTestUser(UUID.randomUUID()),
                friend = createTestUser(UUID.randomUUID()),
                friendAlias = "alias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendshipRepository.findAll(pageable)).thenReturn(slice)
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(domain)
        }
    }

    @Nested
    @DisplayName("findByOwnerEmailAndFriendId 메서드는")
    inner class FindByOwnerEmailAndFriendIdTest {
        @Test
        @DisplayName("소유자 이메일과 친구 ID로 친구 관계를 조회한다")
        fun success() {
            val ownerEmail = "owner@test.com"
            val friendId = UUID.randomUUID()

            val entity = FriendshipJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(friendId),
                "alias"
            )
            val domain = Friendship(
                id = UUID.randomUUID(),
                owner = createTestUser(UUID.randomUUID()),
                friend = createTestUser(friendId),
                friendAlias = "alias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            `when`(springDataFriendshipRepository.findByOwnerUserEmailAndFriendUserId(ownerEmail, friendId)).thenReturn(
                Optional.of(entity)
            )
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.findByOwnerEmailAndFriendId(ownerEmail, friendId)

            assertThat(result).isEqualTo(domain)
        }

        @Test
        @DisplayName("존재하지 않으면 null을 반환한다")
        fun null_when_not_found() {
            val ownerEmail = "owner@test.com"
            val friendId = UUID.randomUUID()

            `when`(springDataFriendshipRepository.findByOwnerUserEmailAndFriendUserId(ownerEmail, friendId)).thenReturn(
                Optional.empty()
            )

            val result = friendshipRepositoryImpl.findByOwnerEmailAndFriendId(ownerEmail, friendId)

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("delete 메서드는")
    inner class DeleteTest {
        @Test
        @DisplayName("소유자와 친구 ID로 양방향 친구 관계를 모두 삭제한다")
        fun success() {
            val ownerId = UUID.randomUUID()
            val friendId = UUID.randomUUID()

            friendshipRepositoryImpl.delete(ownerId, friendId)

            verify(springDataFriendshipRepository).deleteBoth(ownerId, friendId)
        }
    }

    @Nested
    @DisplayName("updateAlias 메서드는")
    inner class UpdateAliasTest {
        @Test
        @DisplayName("친구 별칭을 업데이트한다")
        fun success() {
            val id = UUID.randomUUID()
            val domain = Friendship(
                id = id,
                owner = createTestUser(UUID.randomUUID()),
                friend = createTestUser(UUID.randomUUID()),
                friendAlias = "newAlias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val entity = FriendshipJpaEntity.create(
                createTestUserEntity(UUID.randomUUID()),
                createTestUserEntity(UUID.randomUUID()),
                "oldAlias"
            ).apply { ReflectionTestUtils.setField(this, "id", id) }

            `when`(entityManager.getReference(FriendshipJpaEntity::class.java, id)).thenReturn(entity)
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.updateAlias(domain)

            assertThat(entity.friendAlias).isEqualTo("newAlias")
            assertThat(result).isEqualTo(domain)
        }
    }

    @Nested
    @DisplayName("save 메서드는")
    inner class SaveTest {
        @Test
        @DisplayName("친구 관계를 성공적으로 저장한다")
        fun success() {
            val ownerId = UUID.randomUUID()
            val friendId = UUID.randomUUID()

            val owner = createTestUser(ownerId)
            val friend = createTestUser(friendId)

            val ownerEntity = createTestUserEntity(ownerId)
            val friendEntity = createTestUserEntity(friendId)

            val domain = Friendship(
                id = UUID.randomUUID(),
                owner = owner,
                friend = friend,
                friendAlias = "alias",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )

            val entity = FriendshipJpaEntity.create(ownerEntity, friendEntity, "alias")

            `when`(entityManager.getReference(UserJpaEntity::class.java, ownerId)).thenReturn(ownerEntity)
            `when`(entityManager.getReference(UserJpaEntity::class.java, friendId)).thenReturn(friendEntity)
            `when`(springDataFriendshipRepository.save(any())).thenReturn(entity)
            `when`(friendshipMapper.toDomain(entity)).thenReturn(domain)

            val result = friendshipRepositoryImpl.save(domain)

            assertThat(result).isEqualTo(domain)
            verify(springDataFriendshipRepository).save(any())
        }
    }

    @Test
    @DisplayName("친구 관계 존재 여부를 성공적으로 조회한다")
    fun success() {
        val ownerId = UUID.randomUUID()
        val friendId = UUID.randomUUID()

        `when`(springDataFriendshipRepository.existsByOwnerUserIdAndFriendUserId(any(), any())).thenReturn(true)

        friendshipRepositoryImpl.existsByOwnerUserIdAndFriendUserId(ownerId, friendId)

        verify(springDataFriendshipRepository).existsByOwnerUserIdAndFriendUserId(any(), any())
    }
}
