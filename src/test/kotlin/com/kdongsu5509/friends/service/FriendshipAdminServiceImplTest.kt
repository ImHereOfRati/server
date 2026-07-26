package com.kdongsu5509.friends.service

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendshipAdminServiceImplTest {

    @Mock
    lateinit var friendshipRepository: FriendshipRepository

    @InjectMocks
    lateinit var friendshipAdminServiceImpl: FriendshipAdminServiceImpl

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
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("모든 친구 관계 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val friendship = createTestFriendship()
            val slice = PageImpl(listOf(friendship), pageable, 1L)

            `when`(friendshipRepository.findAll(pageable)).thenReturn(slice)

            val result = friendshipAdminServiceImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendship)
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

            friendshipAdminServiceImpl.deleteById(id)

            verify(friendshipRepository).delete(ownerId, friendId)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 예외를 발생시킨다")
        fun notFound() {
            val id = UUID.randomUUID()
            `when`(friendshipRepository.findById(id)).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendshipAdminServiceImpl.deleteById(id)
            }
        }
    }
}
