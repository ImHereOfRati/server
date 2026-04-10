package com.kdongsu5509.user.application.service.user

import com.kdongsu5509.user.application.port.out.user.CachePort
import com.kdongsu5509.user.application.port.out.user.UserUpdatePort
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AdminUserManagementServiceTest {

    @Mock
    lateinit var cachePort: CachePort

    @Mock
    lateinit var userUpdatePort: UserUpdatePort

    @InjectMocks
    lateinit var adminUserManagementService: AdminUserManagementService

    companion object {
        const val USER_EMAIL = "target@kakao.com"
        const val REDIS_REFRESH_KEY = "refresh:$USER_EMAIL"
    }

    @Test
    @DisplayName("강제 로그아웃 시 Redis에서 리프레시 토큰 키를 삭제한다")
    fun forceLogout_deletesRefreshTokenFromRedis() {
        // when
        adminUserManagementService.forceLogout(USER_EMAIL)

        // then
        verify(cachePort, times(1)).delete(REDIS_REFRESH_KEY)
    }

    @Test
    @DisplayName("유저 차단 시 userUpdatePort.block()을 호출한다")
    fun blockUser_callsUpdatePort() {
        // when
        adminUserManagementService.blockUser(USER_EMAIL)

        // then
        verify(userUpdatePort, times(1)).block(USER_EMAIL)
    }

    @Test
    @DisplayName("유저 차단 해제 시 userUpdatePort.unblock()을 호출한다")
    fun unblockUser_callsUpdatePort() {
        // when
        adminUserManagementService.unblockUser(USER_EMAIL)

        // then
        verify(userUpdatePort, times(1)).unblock(USER_EMAIL)
    }
}
