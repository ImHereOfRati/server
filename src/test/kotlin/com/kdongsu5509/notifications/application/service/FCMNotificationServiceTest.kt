package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.type.NotFoundException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FCMNotificationServiceTest {

    @Mock
    private lateinit var firebasePort: FirebasePort

    @Mock
    private lateinit var fcmTokenPersistencePort: FcmTokenPersistencePort

    @Mock
    private lateinit var notificationHistoryPersistencePort: NotificationHistoryPersistencePort

    private lateinit var service: FCMNotificationService

    @BeforeEach
    fun setUp() {
        service = FCMNotificationService(
            firebasePort,
            fcmTokenPersistencePort,
            notificationHistoryPersistencePort
        )
    }

    @Test
    @DisplayName("정상적으로 FCM 메시지를 전송하고 이력을 저장한다")
    fun send_success() {
        val fcmToken = FcmToken(id = 1L, email = "receiver@example.com", fcmToken = "token-123", deviceType = DeviceType.AOS)
        whenever(fcmTokenPersistencePort.findByUserEmail("receiver@example.com")).thenReturn(fcmToken)

        service.send(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            receiverEmail = "receiver@example.com",
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            extraData = emptyMap()
        )

        verify(firebasePort).send(any(), any(), any(), any())
        verify(notificationHistoryPersistencePort).save(any())
    }

    @Test
    @DisplayName("수신자의 토큰을 찾을 수 없으면 예외가 발생한다")
    fun send_notFoundToken() {
        whenever(fcmTokenPersistencePort.findByUserEmail("receiver@example.com")).thenReturn(null)

        assertThatThrownBy {
            service.send(
                senderNickname = "sender",
                senderEmail = "sender@example.com",
                receiverEmail = "receiver@example.com",
                type = NotificationType.FRIEND_REQUEST_RECEIVED,
                extraData = emptyMap()
            )
        }.isInstanceOf(NotFoundException::class.java)

        verify(firebasePort, never()).send(any(), any(), any(), any())
    }

    @Test
    @DisplayName("미등록 토큰으로 인한 예외 발생 시 토큰을 삭제한다")
    fun send_unregisteredToken() {
        val fcmToken = FcmToken(id = 1L, email = "receiver@example.com", fcmToken = "token-123", deviceType = DeviceType.AOS)
        whenever(fcmTokenPersistencePort.findByUserEmail("receiver@example.com")).thenReturn(fcmToken)

        whenever(firebasePort.send(any(), any(), any(), any())).thenThrow(UnregisteredTokenException())

        service.send(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            receiverEmail = "receiver@example.com",
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            extraData = emptyMap()
        )

        verify(fcmTokenPersistencePort).deleteById(1L)
        verify(notificationHistoryPersistencePort, never()).save(any())
    }
}
