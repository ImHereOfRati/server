package com.kdongsu5509.friends.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
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
class FriendRestrictionServiceImplTest {

    @Mock
    lateinit var friendRestrictionRepository: FriendRestrictionRepository

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var friendshipRepository: FriendshipRepository

    @Mock
    lateinit var friendRequestRepository: FriendRequestRepository

    @InjectMocks
    lateinit var friendRestrictionServiceImpl: FriendRestrictionServiceImpl

    private fun createTestUser(id: UUID = UUID.randomUUID(), email: String = "test@test.com"): User = User(
        id = id,
        email = email,
        nickname = "test",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun createTestRestriction(
        id: UUID = UUID.randomUUID(),
        restrictor: User = createTestUser(),
        restricted: User = createTestUser()
    ): FriendRestriction = FriendRestriction(
        id = id,
        restrictor = restrictor,
        restricted = restricted,
        type = FriendRestrictionType.BLOCK,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("findAllByRestrictorEmail 메서드는")
    inner class FindAllByRestrictorEmailTest {
        @Test
        @DisplayName("차단자의 이메일로 차단 목록 슬라이스를 반환한다")
        fun success() {
            val email = "actor@test.com"
            val pageable = PageRequest.of(0, 10)
            val restriction = createTestRestriction()
            val slice = PageImpl(listOf(restriction), pageable, 1L)

            `when`(friendRestrictionRepository.findAllByEmail(email, pageable)).thenReturn(slice)

            val result = friendRestrictionServiceImpl.findAllByRestrictorEmail(email, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(restriction)
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 차단 목록 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val restriction = createTestRestriction()
            val slice = PageImpl(listOf(restriction), pageable, 1L)

            `when`(friendRestrictionRepository.findAll(pageable)).thenReturn(slice)

            val result = friendRestrictionServiceImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(restriction)
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("ID로 차단 관계를 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            friendRestrictionServiceImpl.deleteById(id)
            verify(friendRestrictionRepository).deleteById(id)
        }
    }

    @Nested
    @DisplayName("deleteByIdAndRestrictorEmail 메서드는")
    inner class DeleteByIdAndRestrictorEmailTest {
        @Test
        @DisplayName("권한이 있는 사용자가 차단 관계를 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "actor@test.com"
            val restrictor = createTestUser(email = email)
            val restriction = createTestRestriction(id = id, restrictor = restrictor)

            `when`(friendRestrictionRepository.findById(id)).thenReturn(restriction)

            friendRestrictionServiceImpl.deleteByIdAndRestrictorEmail(id, email)

            verify(friendRestrictionRepository).deleteById(id)
        }

        @Test
        @DisplayName("존재하지 않는 차단 관계면 예외를 발생시킨다")
        fun notFound() {
            val id = UUID.randomUUID()
            `when`(friendRestrictionRepository.findById(id)).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendRestrictionServiceImpl.deleteByIdAndRestrictorEmail(id, "test@test.com")
            }
        }

        @Test
        @DisplayName("권한이 없는 사용자면 예외를 발생시킨다")
        fun ownerMissMatch() {
            val id = UUID.randomUUID()
            val restrictor = createTestUser(email = "real@test.com")
            val restriction = createTestRestriction(id = id, restrictor = restrictor)

            `when`(friendRestrictionRepository.findById(id)).thenReturn(restriction)

            assertThrows<ImHereBaseException> {
                friendRestrictionServiceImpl.deleteByIdAndRestrictorEmail(id, "fake@test.com")
            }
        }
    }

    @Nested
    @DisplayName("unblockByRestrictorEmailAndRestrictedId 메서드는")
    inner class UnblockByRestrictorEmailAndRestrictedIdTest {
        @Test
        @DisplayName("차단자의 이메일과 차단된 자의 ID로 차단을 해제한다")
        fun success() {
            val email = "actor@test.com"
            val targetId = UUID.randomUUID()

            friendRestrictionServiceImpl.unblockByRestrictorEmailAndRestrictedId(email, targetId)

            verify(friendRestrictionRepository).deleteBlockByRestrictorEmailAndRestrictedId(email, targetId)
        }
    }

    @Nested
    @DisplayName("restrictUser 메서드는")
    inner class RestrictUserTest {
        @Test
        @DisplayName("관련된 친구 및 요청 관계를 모두 지우고 차단을 추가한다")
        fun success() {
            val restrictorId = UUID.randomUUID()
            val restrictedId = UUID.randomUUID()
            val restrictorEmail = "actor@test.com"

            val restrictor = createTestUser(id = restrictorId, email = restrictorEmail)
            val restricted = createTestUser(id = restrictedId)
            val restriction = createTestRestriction(restrictor = restrictor, restricted = restricted)

            `when`(userRepository.findByEmail(restrictorEmail)).thenReturn(restrictor)
            `when`(userRepository.findById(restrictedId)).thenReturn(restricted)
            `when`(friendRestrictionRepository.save(any())).thenReturn(restriction)

            val result = friendRestrictionServiceImpl.restrictUser(restrictorEmail, restrictedId)

            assertThat(result).isEqualTo(restriction)
            verify(friendshipRepository).delete(restrictorId, restrictedId)
            verify(friendRequestRepository).deleteBetween(restrictorId, restrictedId)
            verify(friendRestrictionRepository).save(any())
        }

        @Test
        @DisplayName("차단하는 사용자를 찾을 수 없으면 예외를 발생시킨다")
        fun restrictorNotFound() {
            `when`(userRepository.findByEmail("test@test.com")).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendRestrictionServiceImpl.restrictUser("test@test.com", UUID.randomUUID())
            }
        }

        @Test
        @DisplayName("차단 당할 사용자를 찾을 수 없으면 예외를 발생시킨다")
        fun restrictedNotFound() {
            val restrictor = createTestUser()
            `when`(userRepository.findByEmail("test@test.com")).thenReturn(restrictor)
            `when`(userRepository.findById(any())).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendRestrictionServiceImpl.restrictUser("test@test.com", UUID.randomUUID())
            }
        }
    }

    @Nested
    @DisplayName("existRestricted 메서드는")
    inner class ExistRestrictedTest {
        @Test
        @DisplayName("차단된 관계가 존재하는지 확인한다 (존재할 경우 true)")
        fun existTrue() {
            val restrictorEmail = "actor@test.com"
            val targetId = UUID.randomUUID()
            val targetEmail = "target@test.com"
            val targetUser = createTestUser(id = targetId, email = targetEmail)

            `when`(userRepository.findById(targetId)).thenReturn(targetUser)
            `when`(friendRestrictionRepository.existsRestriction(restrictorEmail, targetEmail)).thenReturn(true)

            val result = friendRestrictionServiceImpl.existRestricted(restrictorEmail, targetId)

            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("차단된 관계가 없는지 확인한다 (없을 경우 false)")
        fun existFalse() {
            val restrictorEmail = "actor@test.com"
            val targetId = UUID.randomUUID()
            val targetEmail = "target@test.com"
            val targetUser = createTestUser(id = targetId, email = targetEmail)

            `when`(userRepository.findById(targetId)).thenReturn(targetUser)
            `when`(friendRestrictionRepository.existsRestriction(restrictorEmail, targetEmail)).thenReturn(false)

            val result = friendRestrictionServiceImpl.existRestricted(restrictorEmail, targetId)

            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("대상이 존재하지 않으면 false를 반환한다")
        fun targetNotFound() {
            val targetId = UUID.randomUUID()
            `when`(userRepository.findById(targetId)).thenReturn(null)

            val result = friendRestrictionServiceImpl.existRestricted("test@test.com", targetId)

            assertThat(result).isFalse()
        }
    }
}
