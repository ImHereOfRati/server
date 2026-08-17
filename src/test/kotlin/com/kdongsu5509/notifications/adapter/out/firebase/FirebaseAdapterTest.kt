package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.domain.RenderedNotification
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.notifications.exception.RetryableFcmException
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.type.InternalServerException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
class FirebaseAdapterTest {

    @Mock
    private lateinit var firebaseMessaging: FirebaseMessaging

    private lateinit var adapter: FirebaseAdapter

    @BeforeEach
    fun setUp() {
        adapter = FirebaseAdapter(firebaseMessaging)
    }

    private fun rendered(
        type: NotificationType = NotificationType.FRIEND_REQUEST_RECEIVED,
    ): RenderedNotification = NotificationTemplate.render(
        type = type,
        senderAlias = "홍길동",
    )

    private fun field(target: Any, name: String): Any? =
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)

    private fun captureSentMessage(): Message {
        val captor = argumentCaptor<Message>()
        verify(firebaseMessaging).send(captor.capture())
        return captor.firstValue
    }

    @Test
    @DisplayName("토큰이 비어있으면 발송을 중단한다")
    fun send_emptyToken() {
        adapter.send("", DeviceType.AOS, rendered())
        verify(firebaseMessaging, never()).send(any<Message>())
    }

    @Test
    @DisplayName("토큰이 존재하면 정상적으로 발송된다")
    fun send_success() {
        whenever(firebaseMessaging.send(any<Message>())).thenReturn("message-id")
        adapter.send("valid_token", DeviceType.AOS, rendered())
        verify(firebaseMessaging).send(any<Message>())
    }

    @Test
    @DisplayName("안드로이드 기기에는 Android 설정만 붙인다")
    fun send_success_aos_uses_android_config_only() {
        // given
        whenever(firebaseMessaging.send(any<Message>())).thenReturn("message-id")

        // when
        adapter.send("valid_token", DeviceType.AOS, rendered())

        // then
        val message = captureSentMessage()
        assertThat(field(message, "androidConfig")).isNotNull()
        assertThat(field(message, "apnsConfig")).isNull()
    }

    @Test
    @DisplayName("iOS 기기에는 APNs 설정만 붙인다")
    fun send_success_ios_uses_apns_config_only() {
        // given
        whenever(firebaseMessaging.send(any<Message>())).thenReturn("message-id")

        // when
        adapter.send("valid_token", DeviceType.IOS, rendered())

        // then
        val message = captureSentMessage()
        assertThat(field(message, "apnsConfig")).isNotNull()
        assertThat(field(message, "androidConfig")).isNull()
    }

    @Test
    @DisplayName("UNREGISTERED 에러 발생 시 UnregisteredTokenException을 던진다")
    fun send_unregistered() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.UNREGISTERED)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(UnregisteredTokenException::class.java)
    }

    @Test
    @DisplayName("INVALID_ARGUMENT 에러는 FCM-900 서버 오류로 다룬다")
    fun send_invalidArgument() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.INVALID_ARGUMENT)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(InternalServerException::class.java)
            .extracting("errorCode")
            .isEqualTo(NotificationException.FCM_INVALID_ARGUMENT)
    }

    @Test
    @DisplayName("발신자 불일치 에러 발생 시 InternalServerException을 던진다")
    fun send_senderIdMismatch() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.SENDER_ID_MISMATCH)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(InternalServerException::class.java)
    }

    @Test
    @DisplayName("서버 에러 등 Retryable 예외 발생 시 RetryableFcmException을 던진다")
    fun send_retryable() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.UNAVAILABLE)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(RetryableFcmException::class.java)
    }

    @Test
    @DisplayName("타사 인증 오류 발생 시 InternalServerException을 던진다")
    fun send_thirdPartyAuth() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(InternalServerException::class.java)
    }

    @Test
    @DisplayName("그 외의 알 수 없는 FCM 에러 시 InternalServerException을 던진다")
    fun send_unknown() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(null) // 혹은 지정되지 않은 에러 코드
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", DeviceType.AOS, rendered()) }
            .isInstanceOf(InternalServerException::class.java)
    }
}
