package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.port.out.user.UserUpdatePort
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class UserNicknameUpdateServiceTest {
    @Mock
    lateinit var userUpdatePort: UserUpdatePort

    @InjectMocks
    lateinit var userNicknameUpdateService: UserNicknameUpdateService

    @Test
    @DisplayName("새로운 닉네임으로 변경 정상 수행")
    fun changeNickName_success() {
        //given
        val testEmail = "test@kakao.com"
        val testNewNickname = "new_nick_name"
        `when`(userUpdatePort.updateNickname(testEmail, testNewNickname)).thenReturn(
            User(
                email = testEmail,
                nickname = testNewNickname,
                oauthProvider = OAuth2Provider.KAKAO,
                role = UserRole.NORMAL,
                status = UserStatus.ACTIVE
            )
        )

        //when
        userNicknameUpdateService.changeNickName(testEmail, testNewNickname)

        //then
        verify(userUpdatePort, times(1)).updateNickname(testEmail, testNewNickname)
    }

    @Test
    @DisplayName("userUpdatePort에서 오류 발생 시 정상적으로 전파 된다")
    fun changeNickName_fail() {
        //given
        val testEmail = "failure@kakao.com"
        val testNewNickname = "fail"
        `when`(
            userUpdatePort.updateNickname(
                testEmail,
                testNewNickname
            )
        ).thenThrow(BusinessException(ErrorCode.USER_NOT_FOUND))

        //when, then
        Assertions.assertThatThrownBy {
            userNicknameUpdateService.changeNickName(testEmail, testNewNickname)
        }.isInstanceOf(BusinessException::class.java)
            .hasMessage(ErrorCode.USER_NOT_FOUND.message)
    }
}