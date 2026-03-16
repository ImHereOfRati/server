package com.kdongsu5509.imhere.notification.application.service

import com.kdongsu5509.imhere.common.exception.domain.notification.FcmTokenNotFoundException
import com.kdongsu5509.imhere.notification.application.domain.FcmToken
import com.kdongsu5509.imhere.notification.application.port.out.FindTokenPort
import com.kdongsu5509.imhere.notification.application.port.out.FirebasePort
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class SelfNotificationServiceTest {
    @Mock
    private lateinit var findTokenPort: FindTokenPort

    @Mock
    private lateinit var firebasePort: FirebasePort

    @InjectMocks
    private lateinit var selfNotificationService: SelfNotificationService

    @Test
    @DisplayName("사용자의 토큰 정보가 존재하면 sendToMe가 잘 작동된다")
    fun sendToMe_success() {
        //given
        val testEmail = "dongsu@test.com"
        val testToken = "testToken"
        val testFcmToken = FcmToken(
            userEmail = testEmail,
            fcmToken = testToken
        )

        `when`(findTokenPort.findByUserEmail(testEmail)).thenReturn(
            testFcmToken
        )

        //when, then
        assertDoesNotThrow {
            selfNotificationService.sendToMe(testEmail)
        }

        verify(findTokenPort, times(1)).findByUserEmail(testEmail)
        verify(firebasePort, times(1)).send(testToken)
    }

    @Test
    @DisplayName("사용자의 토큰 정보가 없으면 FcmTokenNotFoundException 가 발생한다")
    fun sendToMe_fail() {
        //given
        val testEmail = "dongsu@test.com"
        `when`(findTokenPort.findByUserEmail(testEmail)).thenReturn(
            null
        )

        //when, then
        assertThatThrownBy {
            selfNotificationService.sendToMe(testEmail)
        }
            .isInstanceOf(FcmTokenNotFoundException::class.java)
            .hasMessage("사용자의 FCM 토큰을 찾을 수 없습니다")
    }
}