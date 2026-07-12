package com.kdongsu5509.friends.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.dto.UserResult
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestPolicyTest {

    @Mock
    lateinit var friendRequestRepository: FriendRequestRepository

    @Mock
    lateinit var friendRestrictionRepository: FriendRestrictionRepository

    @Mock
    lateinit var friendshipRepository: FriendshipRepository

    @InjectMocks
    lateinit var friendRequestPolicy: FriendRequestPolicy

    private fun createUserResult(
        id: UUID = UUID.randomUUID(),
        email: String = "test@test.com",
        nickname: String = "test"
    ): UserResult = UserResult(
        id = id,
        email = email,
        nickname = nickname,
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Nested
    @DisplayName("verifyRequestable 메서드는")
    inner class VerifyRequestableTest {
        @Test
        @DisplayName("차단·중복요청·기존친구 관계가 전부 없으면 통과한다")
        fun success() {
            val me = createUserResult(email = "req@test.com")
            val target = createUserResult(email = "rec@test.com")

            `when`(friendRestrictionRepository.existsRestriction(me.email, target.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(target.email, me.email)).thenReturn(false)
            `when`(friendRequestRepository.existsByRequesterIdAndReceiverId(me.id, target.id)).thenReturn(false)
            `when`(friendshipRepository.existsByOwnerUserIdAndFriendUserId(me.id, target.id)).thenReturn(false)

            friendRequestPolicy.verifyRequestable(me, target)
        }

        @Test
        @DisplayName("내가 상대를 차단한 상태면 예외를 발생시킨다")
        fun blockByMe() {
            val me = createUserResult(email = "req@test.com")
            val target = createUserResult(email = "rec@test.com")

            `when`(friendRestrictionRepository.existsRestriction(me.email, target.email)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestPolicy.verifyRequestable(me, target)
            }
        }

        @Test
        @DisplayName("상대가 나를 차단한 상태면 예외를 발생시킨다")
        fun blockByTarget() {
            val me = createUserResult(email = "req@test.com")
            val target = createUserResult(email = "rec@test.com")

            `when`(friendRestrictionRepository.existsRestriction(me.email, target.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(target.email, me.email)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestPolicy.verifyRequestable(me, target)
            }
        }

        @Test
        @DisplayName("이미 친구 요청을 보낸 상태이면 예외를 발생시킨다")
        fun alreadyRequested() {
            val me = createUserResult(email = "req@test.com")
            val target = createUserResult(email = "rec@test.com")

            `when`(friendRestrictionRepository.existsRestriction(me.email, target.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(target.email, me.email)).thenReturn(false)
            `when`(friendRequestRepository.existsByRequesterIdAndReceiverId(me.id, target.id)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestPolicy.verifyRequestable(me, target)
            }
        }

        @Test
        @DisplayName("상대와 이미 친구 상태이면 예외를 발생시킨다")
        fun alreadyFriend() {
            val me = createUserResult(email = "req@test.com")
            val target = createUserResult(email = "rec@test.com")

            `when`(friendRestrictionRepository.existsRestriction(me.email, target.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(target.email, me.email)).thenReturn(false)
            `when`(friendRequestRepository.existsByRequesterIdAndReceiverId(me.id, target.id)).thenReturn(false)
            `when`(friendshipRepository.existsByOwnerUserIdAndFriendUserId(me.id, target.id)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestPolicy.verifyRequestable(me, target)
            }
        }
    }
}
