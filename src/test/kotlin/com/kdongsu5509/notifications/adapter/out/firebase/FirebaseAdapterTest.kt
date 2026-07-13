package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.type.InternalServerException
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FirebaseAdapterTest {

    @Mock
    private lateinit var firebaseMessaging: FirebaseMessaging

    private lateinit var adapter: FirebaseAdapter

    @BeforeEach
    fun setUp() {
        adapter = FirebaseAdapter(firebaseMessaging)
    }

    @Test
    @DisplayName("토큰이 비어있으면 발송을 중단한다")
    fun send_emptyToken() {
        adapter.send("", "title", "body", emptyMap())
        verify(firebaseMessaging, never()).send(any<Message>())
    }

    @Test
    @DisplayName("토큰이 존재하면 정상적으로 발송된다")
    fun send_success() {
        whenever(firebaseMessaging.send(any<Message>())).thenReturn("message-id")
        adapter.send("valid_token", "title", "body", mapOf("key" to "value"))
        verify(firebaseMessaging).send(any<Message>())
    }

    @Test
    @DisplayName("UNREGISTERED 에러 발생 시 UnregisteredTokenException을 던진다")
    fun send_unregistered() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.UNREGISTERED)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(UnregisteredTokenException::class.java)
    }

    @Test
    @DisplayName("INVALID_ARGUMENT 에러 발생 시 InvalidInputException을 던진다")
    fun send_invalidArgument() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.INVALID_ARGUMENT)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("발신자 불일치 에러 발생 시 InternalServerException을 던진다")
    fun send_senderIdMismatch() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.SENDER_ID_MISMATCH)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(InternalServerException::class.java)
    }

    @Test
    @DisplayName("서버 에러 등 Retryable 예외 발생 시 RetryableFcmException을 던진다")
    fun send_retryable() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.UNAVAILABLE)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(RetryableFcmException::class.java)
    }

    @Test
    @DisplayName("타사 인증 오류 발생 시 InternalServerException을 던진다")
    fun send_thirdPartyAuth() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(MessagingErrorCode.THIRD_PARTY_AUTH_ERROR)
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(InternalServerException::class.java)
    }

    @Test
    @DisplayName("그 외의 알 수 없는 FCM 에러 시 InternalServerException을 던진다")
    fun send_unknown() {
        val ex = Mockito.mock(FirebaseMessagingException::class.java)
        whenever(ex.messagingErrorCode).thenReturn(null) // 혹은 지정되지 않은 에러 코드
        whenever(firebaseMessaging.send(any<Message>())).thenThrow(ex)

        assertThatThrownBy { adapter.send("token", "title", "body", emptyMap()) }
            .isInstanceOf(InternalServerException::class.java)
    }
}
