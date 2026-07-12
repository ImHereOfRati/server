package com.kdongsu5509.friends.service

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.repository.FriendRestrictionRepository
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
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
import java.util.*

@ExtendWith(MockitoExtension::class)
class FriendRestrictionAdminServiceImplTest {

    @Mock
    lateinit var friendRestrictionRepository: FriendRestrictionRepository

    @InjectMocks
    lateinit var friendRestrictionAdminServiceImpl: FriendRestrictionAdminServiceImpl

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
        @DisplayName("전체 차단 목록 슬라이스를 반환한다")
        fun success() {
            val pageable = PageRequest.of(0, 10)
            val restriction = FriendRestriction.block(
                restrictor = createTestUser("restrictor@test.com"),
                restricted = createTestUser("restricted@test.com")
            )
            val slice = PageImpl(listOf(restriction), pageable, 1L)

            `when`(friendRestrictionRepository.findAll(pageable)).thenReturn(slice)

            val result = friendRestrictionAdminServiceImpl.findAll(pageable)

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
            friendRestrictionAdminServiceImpl.deleteById(id)
            verify(friendRestrictionRepository).deleteById(id)
        }
    }
}
