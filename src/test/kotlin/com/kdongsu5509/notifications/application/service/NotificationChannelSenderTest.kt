package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.*
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

/**
 * 방어 대상은 FCM 토큰의 생명주기다. 쓸모없다고 판명된 토큰을 지우지 않으면
 * 같은 토큰으로 계속 실패하며 재시도만 태우고 DB에 쓰레기가 쌓인다.
 */
class NotificationChannelSenderTest {
    private val firebasePort: FirebasePort = mock()
    private val fcmTokenPersistencePort: FcmTokenPersistencePort = mock()
    private val externalMessagePort: ExternalMessagePort = mock()

    private val sender = NotificationChannelSender(firebasePort, fcmTokenPersistencePort, externalMessagePort)

    private val receiverId = UUID.randomUUID()

    @Test
    @DisplayName("등록된 토큰이 없으면 외부 채널을 부르지 않는다")
    fun missing_token_stops_before_the_channel() {
        whenever(fcmTokenPersistencePort.findByOwnerId(receiverId)).thenReturn(null)

        assertThatThrownBy { sender.sendViaExternalMethod(fcmNotification()) }
            .isInstanceOf(RuntimeException::class.java)

        verify(firebasePort, never()).send(any(), any(), any())
        verify(fcmTokenPersistencePort, never()).deleteById(any())
    }

    @Test
    @DisplayName("등록 해제된 토큰이면 지우고 예외를 그대로 올려보낸다")
    fun unregistered_token_is_deleted_and_error_propagates() {
        whenever(fcmTokenPersistencePort.findByOwnerId(receiverId)).thenReturn(token("살아있어 보이는 토큰"))
        whenever(firebasePort.send(any(), any(), any())).thenThrow(UnregisteredTokenException())

        assertThatThrownBy { sender.sendViaExternalMethod(fcmNotification()) }
            .isInstanceOf(UnregisteredTokenException::class.java)

        verify(fcmTokenPersistencePort).deleteById(TOKEN_ID)
    }

    @Test
    @DisplayName("발송에 성공한 토큰은 지우지 않는다")
    fun healthy_token_survives() {
        whenever(fcmTokenPersistencePort.findByOwnerId(receiverId)).thenReturn(token("정상 토큰"))

        sender.sendViaExternalMethod(fcmNotification())

        verify(fcmTokenPersistencePort, never()).deleteById(any())
    }

    private fun token(value: String): FcmToken =
        FcmToken(id = TOKEN_ID, ownerId = receiverId, fcmToken = value, deviceType = DeviceType.AOS)

    private fun fcmNotification(): Notification =
        Notification.reconstruct(
            id = 1L,
            dedupeKey = "dedupe",
            targetIdentifier = receiverId.toString(),
            method = NotificationMethod.FCM,
            senderAlias = "보낸이",
            type = NotificationType.FRIEND_REQUEST_RECEIVED,
            title = "제목",
            body = "본문",
            extraData = emptyMap(),
            status = NotificationStatus.PENDING,
            attempts = 0,
            lastError = null,
            sentAt = null,
            isRead = false,
            createdAt = LocalDateTime.now(),
        )

    private companion object {
        const val TOKEN_ID = 10L
    }
}
