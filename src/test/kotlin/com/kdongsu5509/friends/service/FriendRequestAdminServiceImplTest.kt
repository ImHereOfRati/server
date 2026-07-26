package com.kdongsu5509.friends.service

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.repository.FriendRequestRepository
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.domain.User
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRequestAdminServiceImplTest {

    @Mock
    lateinit var friendRequestRepository: FriendRequestRepository

    @InjectMocks
    lateinit var friendRequestAdminServiceImpl: FriendRequestAdminServiceImpl

    private fun createTestUser(email: String): User = User(
        id = UUID.randomUUID(),
        email = email,
        nickname = "test",
        role = UserRole.NORMAL,
        oauthProvider = OAuth2Provider.KAKAO,
        status = UserStatus.ACTIVE
    )

    @Nested
    @DisplayName("findAll 메서드는")
    inner class FindAllTest {
        @Test
        @DisplayName("전체 친구 요청 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val friendRequest = FriendRequest(
                id = UUID.randomUUID(),
                requester = createTestUser("req@test.com"),
                receiver = createTestUser("rec@test.com"),
                message = "안녕",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            val slice = PageImpl(listOf(friendRequest), pageable, 1L)

            `when`(friendRequestRepository.findAll(pageable)).thenReturn(slice)

            val result = friendRequestAdminServiceImpl.findAll(pageable)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(friendRequest)
        }
    }

    @Nested
    @DisplayName("deleteById 메서드는")
    inner class DeleteByIdTest {
        @Test
        @DisplayName("ID로 친구 요청을 삭제한다")
        fun success() {
            val id = UUID.randomUUID()
            friendRequestAdminServiceImpl.deleteById(id)
            verify(friendRequestRepository).deleteById(id)
        }
    }
}
