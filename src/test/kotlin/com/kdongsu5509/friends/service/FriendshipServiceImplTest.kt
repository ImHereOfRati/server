package com.kdongsu5509.friends.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendshipServiceImplTest {

    @Mock
    lateinit var friendshipRepository: FriendshipRepository

    @Mock
    lateinit var friendRestrictionRepository: FriendRestrictionRepository

    @InjectMocks
    lateinit var friendshipServiceImpl: FriendshipServiceImpl

    private fun createTestUser(id: UUID = UUID.randomUUID(), email: String = "test@test.com"): User = User(
        id = id,
        email = email,
        nickname = "test",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun createTestFriendship(
        id: UUID = UUID.randomUUID(),
        owner: User = createTestUser(),
        friend: User = createTestUser()
    ): Friendship = Friendship(
        id = id,
        owner = owner,
        friend = friend,
        friendAlias = "alias",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("findAllByOwnerEmail 메서드는")
    inner class FindAllByOwnerEmailTest {
        @Test
        @DisplayName("소유자 이메일로 친구 관계 슬라이스를 반환한다")
        fun success() {
            val email = "owner@test.com"
            val pageable = PageRequest.of(0, 10)
            val friendship = createTestFriendship()
            val slice = PageImpl(listOf(friendship), pageable, 1L)

            `when`(friendshipRepository.findByOwnerEmail(email, pageable)).thenReturn(slice)

            val result = friendshipServiceImpl.findAllByOwnerEmail(email, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendship)
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("모든 친구 관계 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val friendship = createTestFriendship()
            val slice = PageImpl(listOf(friendship), pageable, 1L)

            `when`(friendshipRepository.findAll(pageable)).thenReturn(slice)

            val result = friendshipServiceImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendship)
        }
    }

    @Nested
    @DisplayName("findByOwnerEmailAndFriendId 메서드는")
    inner class FindByOwnerEmailAndFriendIdTest {
        @Test
        @DisplayName("해당하는 친구 관계를 반환한다")
        fun success() {
            val email = "owner@test.com"
            val friendId = UUID.randomUUID()
            val friendship = createTestFriendship()

            `when`(friendshipRepository.findByOwnerEmailAndFriendId(email, friendId)).thenReturn(friendship)

            val result = friendshipServiceImpl.findByOwnerEmailAndFriendId(email, friendId)

            assertThat(result).isEqualTo(friendship)
        }
    }

    @Nested
    @DisplayName("findByIdAndOwnerEmail 메서드는")
    inner class FindByIdAndOwnerEmailTest {
        @Test
        @DisplayName("정상적으로 친구 관계를 찾으면 반환한다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "owner@test.com"
            val owner = createTestUser(email = email)
            val friendship = createTestFriendship(id = id, owner = owner)

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)

            val result = friendshipServiceImpl.findByIdAndOwnerEmail(id, email)

            assertThat(result).isEqualTo(friendship)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 FRIEND_RELATIONSHIP_NOT_FOUND 예외를 던진다")
        fun notFound() {
            val id = UUID.randomUUID()
            `when`(friendshipRepository.findById(id)).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendshipServiceImpl.findByIdAndOwnerEmail(id, "test@test.com")
            }
        }

        @Test
        @DisplayName("조회된 관계의 소유자 이메일이 다르면 FRIEND_RELATIONSHIP_OWNER_MISS_MATCH 예외를 던진다")
        fun ownerMissMatch() {
            val id = UUID.randomUUID()
            val owner = createTestUser(email = "real_owner@test.com")
            val friendship = createTestFriendship(id = id, owner = owner)

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)

            assertThrows<ImHereBaseException> {
                friendshipServiceImpl.findByIdAndOwnerEmail(id, "hacker@test.com")
            }
        }
    }

    @Nested
    @DisplayName("updateAliasByIdAndOwnerEmail 메서드는")
    inner class UpdateAliasByIdAndOwnerEmailTest {
        @Test
        @DisplayName("별칭을 성공적으로 업데이트한다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "owner@test.com"
            val owner = createTestUser(email = email)
            val friendship = createTestFriendship(id = id, owner = owner)
            val updatedFriendship = friendship.updateFriendAlias("newAlias")

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)
            `when`(friendshipRepository.updateAlias(any())).thenReturn(updatedFriendship)

            val result = friendshipServiceImpl.updateAliasByIdAndOwnerEmail(id, email, "newAlias")

            assertThat(result).isEqualTo(updatedFriendship)
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("존재하는 ID에 대해 친구 관계를 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            val ownerId = UUID.randomUUID()
            val friendId = UUID.randomUUID()
            val owner = createTestUser(id = ownerId)
            val friend = createTestUser(id = friendId)
            val friendship = createTestFriendship(id = id, owner = owner, friend = friend)

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)

            friendshipServiceImpl.deleteById(id)

            verify(friendshipRepository).delete(ownerId, friendId)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 예외를 발생시킨다")
        fun notFound() {
            val id = UUID.randomUUID()
            `when`(friendshipRepository.findById(id)).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendshipServiceImpl.deleteById(id)
            }
        }
    }

    @Nested
    @DisplayName("deleteByIdAndOwnerEmail 메서드는")
    inner class DeleteByIdAndOwnerEmailTest {
        @Test
        @DisplayName("권한이 있는 소유자가 친구 관계를 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            val ownerId = UUID.randomUUID()
            val friendId = UUID.randomUUID()
            val email = "owner@test.com"
            val owner = createTestUser(id = ownerId, email = email)
            val friend = createTestUser(id = friendId)
            val friendship = createTestFriendship(id = id, owner = owner, friend = friend)

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)

            friendshipServiceImpl.deleteByIdAndOwnerEmail(id, email)

            verify(friendshipRepository).delete(ownerId, friendId)
        }
    }

    @Nested
    @DisplayName("blockByIdAndOwnerEmail 메서드는")
    inner class BlockByIdAndOwnerEmailTest {
        @Test
        @DisplayName("친구 관계를 삭제하고 차단 목록에 추가한다")
        fun success() {
            val id = UUID.randomUUID()
            val ownerId = UUID.randomUUID()
            val friendId = UUID.randomUUID()
            val email = "owner@test.com"
            val owner = createTestUser(id = ownerId, email = email)
            val friend = createTestUser(id = friendId)
            val friendship = createTestFriendship(id = id, owner = owner, friend = friend)

            `when`(friendshipRepository.findById(id)).thenReturn(friendship)

            friendshipServiceImpl.blockByIdAndOwnerEmail(id, email)

            verify(friendRestrictionRepository).save(any())
            verify(friendshipRepository).delete(ownerId, friendId)
        }
    }
}
