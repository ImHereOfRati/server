package com.kdongsu5509.friends.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.friends.controller.dto.FriendRequestViewType
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.friends.domain.Friendship
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.friends.repository.FriendshipRepository
import com.kdongsu5509.friends.repository.mapper.FriendRequestMapper
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.user.application.port.FriendAlertPort
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.service.UserService
import com.kdongsu5509.user.service.dto.UserResult
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
class FriendRequestServiceImplTest {

    @Mock
    lateinit var userService: UserService

    @Mock
    lateinit var friendRequestRepository: FriendRequestRepository

    @Mock
    lateinit var friendRestrictionRepository: FriendRestrictionRepository

    @Mock
    lateinit var friendshipRepository: FriendshipRepository

    @Mock
    lateinit var friendAlertPort: FriendAlertPort

    @Mock
    lateinit var friendRequestMapper: FriendRequestMapper

    @InjectMocks
    lateinit var friendRequestServiceImpl: FriendRequestServiceImpl

    private fun createTestUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test@test.com",
        nickname: String = "test"
    ): User = User(
        id = id,
        email = email,
        nickname = nickname,
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    private fun createTestUserResult(user: User): UserResult = UserResult(
        id = user.id!!,
        email = user.email,
        nickname = user.nickname,
        role = user.role,
        oauthProvider = user.oauthProvider,
        status = user.status
    )

    private fun createTestFriendRequest(
        id: UUID = UUID.randomUUID(),
        requester: User = createTestUser(),
        receiver: User = createTestUser()
    ): FriendRequest = FriendRequest(
        id = id,
        requester = requester,
        receiver = receiver,
        message = "안녕",
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @Nested
    @DisplayName("request 메서드는")
    inner class RequestTest {
        @Test
        @DisplayName("차단 관계가 없을 때 성공적으로 요청을 생성한다")
        fun success() {
            val requesterEmail = "req@test.com"
            val receiverId = UUID.randomUUID()

            val requesterUser = createTestUser(email = requesterEmail)
            val receiverUser = createTestUser(id = receiverId, email = "rec@test.com")
            val requesterResult = createTestUserResult(requesterUser)
            val receiverResult = createTestUserResult(receiverUser)
            val friendRequest = createTestFriendRequest(requester = requesterUser, receiver = receiverUser)

            `when`(userService.findByEmail(requesterEmail)).thenReturn(requesterResult)
            `when`(userService.findById(receiverId)).thenReturn(receiverResult)
            `when`(friendRestrictionRepository.existsRestriction(requesterEmail, receiverUser.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(receiverUser.email, requesterEmail)).thenReturn(false)
            `when`(friendRequestRepository.save(any())).thenReturn(friendRequest)

            val result = friendRequestServiceImpl.request(requesterEmail, receiverId, "안녕")

            assertThat(result).isEqualTo(friendRequest)
            verify(friendRequestRepository).save(any())
        }

        @Test
        @DisplayName("내가 상대를 차단한 상태면 예외를 발생시킨다")
        fun blockByMe() {
            val requesterEmail = "req@test.com"
            val receiverId = UUID.randomUUID()

            val requesterUser = createTestUser(email = requesterEmail)
            val receiverUser = createTestUser(id = receiverId, email = "rec@test.com")

            `when`(userService.findByEmail(requesterEmail)).thenReturn(createTestUserResult(requesterUser))
            `when`(userService.findById(receiverId)).thenReturn(createTestUserResult(receiverUser))
            `when`(friendRestrictionRepository.existsRestriction(requesterEmail, receiverUser.email)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.request(requesterEmail, receiverId, "안녕")
            }
        }

        @Test
        @DisplayName("상대가 나를 차단한 상태면 예외를 발생시킨다")
        fun blockByTarget() {
            val requesterEmail = "req@test.com"
            val receiverId = UUID.randomUUID()

            val requesterUser = createTestUser(email = requesterEmail)
            val receiverUser = createTestUser(id = receiverId, email = "rec@test.com")

            `when`(userService.findByEmail(requesterEmail)).thenReturn(createTestUserResult(requesterUser))
            `when`(userService.findById(receiverId)).thenReturn(createTestUserResult(receiverUser))
            `when`(friendRestrictionRepository.existsRestriction(requesterEmail, receiverUser.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(receiverUser.email, requesterEmail)).thenReturn(true)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.request(requesterEmail, receiverId, "안녕")
            }
        }

        //verifyNotAlreadyRequested(me, target)
        @Test
        @DisplayName("이미 친구 요청을 보낸 상태이면 예외를 발생시킨다")
        fun alreadyRequested() {
            val requesterEmail = "req@test.com"
            val receiverId = UUID.randomUUID()

            val requesterUser = createTestUser(email = requesterEmail)
            val receiverUser = createTestUser(id = receiverId, email = "rec@test.com")

            `when`(userService.findByEmail(requesterEmail)).thenReturn(createTestUserResult(requesterUser))
            `when`(userService.findById(receiverId)).thenReturn(createTestUserResult(receiverUser))
            `when`(friendRestrictionRepository.existsRestriction(requesterEmail, receiverUser.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(receiverUser.email, requesterEmail)).thenReturn(false)

            `when`(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterUser.id!!, receiverId)).thenReturn(
                true
            )

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.request(requesterEmail, receiverId, "안녕")
            }
        }

        //        verifyNotAlreadyFriend(me, target)
        @Test
        @DisplayName("상대와 이미 친구 상태이면 예외를 발생시킨다")
        fun alreadyFriend() {
            val requesterEmail = "req@test.com"
            val receiverId = UUID.randomUUID()

            val requesterUser = createTestUser(email = requesterEmail)
            val receiverUser = createTestUser(id = receiverId, email = "rec@test.com")

            `when`(userService.findByEmail(requesterEmail)).thenReturn(createTestUserResult(requesterUser))
            `when`(userService.findById(receiverId)).thenReturn(createTestUserResult(receiverUser))
            `when`(friendRestrictionRepository.existsRestriction(requesterEmail, receiverUser.email)).thenReturn(false)
            `when`(friendRestrictionRepository.existsRestriction(receiverUser.email, requesterEmail)).thenReturn(false)
            `when`(friendRequestRepository.existsByRequesterIdAndReceiverId(requesterUser.id!!, receiverId)).thenReturn(
                false
            )
            `when`(friendshipRepository.existsByOwnerUserIdAndFriendUserId(requesterUser.id, receiverId)).thenReturn(
                true
            )

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.request(requesterEmail, receiverId, "안녕")
            }
        }
    }

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 친구 요청 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val friendRequest = createTestFriendRequest()
            val slice = PageImpl(listOf(friendRequest), pageable, 1L)

            `when`(friendRequestRepository.findAll(pageable)).thenReturn(slice)

            val result = friendRequestServiceImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendRequest)
        }
    }

    @Nested
    @DisplayName("findAllByEmailAndType 메서드는")
    inner class FindAllByEmailAndTypeTest {
        @Test
        @DisplayName("SENT 타입일 때 내가 보낸 요청을 조회한다")
        fun sent() {
            val email = "req@test.com"
            val pageable = PageRequest.of(0, 10)
            val friendRequest = createTestFriendRequest()
            val slice = PageImpl(listOf(friendRequest), pageable, 1L)

            `when`(friendRequestRepository.findAllByRequesterEmail(email, pageable)).thenReturn(slice)

            val result = friendRequestServiceImpl.findAllByEmailAndType(email, FriendRequestViewType.SENT, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendRequest)
        }

        @Test
        @DisplayName("RECEIVED 타입일 때 내가 받은 요청을 조회한다")
        fun received() {
            val email = "rec@test.com"
            val pageable = PageRequest.of(0, 10)
            val friendRequest = createTestFriendRequest()
            val slice = PageImpl(listOf(friendRequest), pageable, 1L)

            `when`(friendRequestRepository.findAllByReceiverEmail(email, pageable)).thenReturn(slice)

            val result = friendRequestServiceImpl.findAllByEmailAndType(email, FriendRequestViewType.RECEIVED, pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendRequest)
        }
    }

    @Nested
    @DisplayName("findById 메서드는")
    inner class FindByIdTest {
        @Test
        @DisplayName("정상적으로 친구 요청을 찾으면 반환한다")
        fun success() {
            val id = UUID.randomUUID()
            val friendRequest = createTestFriendRequest(id = id)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            val result = friendRequestServiceImpl.findById(id)

            assertThat(result).isEqualTo(friendRequest)
        }

        @Test
        @DisplayName("존재하지 않는 ID면 예외를 던진다")
        fun notFound() {
            val id = UUID.randomUUID()
            `when`(friendRequestRepository.findById(id)).thenReturn(null)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.findById(id)
            }
        }
    }

    @Nested
    @DisplayName("findByIdAndParticipantEmail 메서드는")
    inner class FindByIdAndParticipantEmailTest {
        @Test
        @DisplayName("내가 발신자일 때 정상 조회한다")
        fun asRequester() {
            val id = UUID.randomUUID()
            val email = "req@test.com"
            val requester = createTestUser(email = email)
            val friendRequest = createTestFriendRequest(id = id, requester = requester)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            val result = friendRequestServiceImpl.findByIdAndParticipantEmail(id, email)

            assertThat(result).isEqualTo(friendRequest)
        }

        @Test
        @DisplayName("내가 수신자일 때 정상 조회한다")
        fun asReceiver() {
            val id = UUID.randomUUID()
            val email = "rec@test.com"
            val receiver = createTestUser(email = email)
            val friendRequest = createTestFriendRequest(id = id, receiver = receiver)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            val result = friendRequestServiceImpl.findByIdAndParticipantEmail(id, email)

            assertThat(result).isEqualTo(friendRequest)
        }

        @Test
        @DisplayName("참여자가 아니면 예외를 발생시킨다")
        fun missMatch() {
            val id = UUID.randomUUID()
            val friendRequest = createTestFriendRequest(id = id)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.findByIdAndParticipantEmail(id, "hacker@test.com")
            }
        }
    }

    @Nested
    @DisplayName("acceptRequest 메서드는")
    inner class AcceptRequestTest {
        @Test
        @DisplayName("수신자가 요청을 수락하면 양방향 친구 관계가 생성되고 요청은 삭제된다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "rec@test.com"
            val receiver = createTestUser(email = email, nickname = "rec_nick")
            val requester = createTestUser(email = "req@test.com", nickname = "req_nick")
            val friendRequest = createTestFriendRequest(id = id, requester = requester, receiver = receiver)
            val savedFriendship = Friendship(
                UUID.randomUUID(),
                receiver,
                requester,
                requester.nickname,
                LocalDateTime.now(),
                LocalDateTime.now()
            )

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)
            `when`(friendshipRepository.save(any())).thenReturn(savedFriendship)

            val result = friendRequestServiceImpl.acceptRequest(email, id)

            assertThat(result).isEqualTo(savedFriendship)
            verify(friendRequestRepository).deleteById(id)
        }

        @Test
        @DisplayName("수신자가 아닌 사람이 수락하려 하면 예외를 발생시킨다")
        fun ownerMissMatch() {
            val id = UUID.randomUUID()
            val friendRequest = createTestFriendRequest(id = id)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.acceptRequest("hacker@test.com", id)
            }
        }
    }

    @Nested
    @DisplayName("rejectRequest 메서드는")
    inner class RejectRequestTest {
        @Test
        @DisplayName("수신자가 요청을 거절하면 차단(거절) 관계가 생성되고 요청은 삭제된다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "rec@test.com"
            val receiver = createTestUser(email = email)
            val requester = createTestUser(email = "req@test.com")
            val friendRequest = createTestFriendRequest(id = id, requester = requester, receiver = receiver)
            val savedRestriction = FriendRestriction(
                UUID.randomUUID(),
                receiver,
                requester,
                FriendRestrictionType.REJECT,
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now()
            )

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)
            `when`(friendRestrictionRepository.save(any())).thenReturn(savedRestriction)

            val result = friendRequestServiceImpl.rejectRequest(email, id)

            assertThat(result).isEqualTo(savedRestriction)
            verify(friendRequestRepository).deleteById(id)
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("ID로 친구 요청을 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            friendRequestServiceImpl.deleteById(id)
            verify(friendRequestRepository).deleteById(id)
        }
    }

    @Nested
    @DisplayName("deleteByIdAndReceiverEmail 메서드는")
    inner class DeleteByIdAndReceiverEmailTest {
        @Test
        @DisplayName("수신자가 친구 요청을 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "rec@test.com"
            val receiver = createTestUser(email = email)
            val friendRequest = createTestFriendRequest(id = id, receiver = receiver)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            friendRequestServiceImpl.deleteByIdAndReceiverEmail(id, email)

            verify(friendRequestRepository).deleteById(id)
        }

        @Test
        @DisplayName("수신자가 다르면 예외를 발생시킨다")
        fun ownerMissMatch() {
            val id = UUID.randomUUID()
            val friendRequest = createTestFriendRequest(id = id)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.deleteByIdAndReceiverEmail(id, "fake@test.com")
            }
        }
    }

    @Nested
    @DisplayName("deleteByIdAndRequesterEmail 메서드는")
    inner class DeleteByIdAndRequesterEmailTest {
        @Test
        @DisplayName("요청자가 친구 요청을 삭제(취소)한다")
        fun success() {
            val id = UUID.randomUUID()
            val email = "req@test.com"
            val requester = createTestUser(email = email)
            val friendRequest = createTestFriendRequest(id = id, requester = requester)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            friendRequestServiceImpl.deleteByIdAndRequesterEmail(id, email)

            verify(friendRequestRepository).deleteById(id)
        }

        @Test
        @DisplayName("요청자가 다르면 예외를 발생시킨다")
        fun ownerMissMatch() {
            val id = UUID.randomUUID()
            val friendRequest = createTestFriendRequest(id = id)

            `when`(friendRequestRepository.findById(id)).thenReturn(friendRequest)

            assertThrows<ImHereBaseException> {
                friendRequestServiceImpl.deleteByIdAndRequesterEmail(id, "fake@test.com")
            }
        }
    }
}
