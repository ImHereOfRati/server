package com.kdongsu5509.user.application.service.friend

import com.kdongsu5509.user.application.port.out.friend.AdminFriendPort
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AdminFriendManagementServiceTest {

    @Mock
    lateinit var adminFriendPort: AdminFriendPort

    @InjectMocks
    lateinit var adminFriendManagementService: AdminFriendManagementService

    companion object {
        const val USER_A = "userA@kakao.com"
        const val USER_B = "userB@kakao.com"
    }

    @Test
    @DisplayName("두 유저의 친구 관계 강제 삭제 시 port를 정확히 호출한다")
    fun forceClearFriendRelationship_callsPort() {
        // when
        adminFriendManagementService.forceClearFriendRelationship(USER_A, USER_B)

        // then
        verify(adminFriendPort, times(1)).forceClearFriendRelationship(USER_A, USER_B)
    }

    @Test
    @DisplayName("친구 요청 강제 삭제 시 port를 정확히 호출한다")
    fun forceClearFriendRequest_callsPort() {
        // when
        adminFriendManagementService.forceClearFriendRequest(USER_A, USER_B)

        // then
        verify(adminFriendPort, times(1)).forceClearFriendRequest(USER_A, USER_B)
    }
}
