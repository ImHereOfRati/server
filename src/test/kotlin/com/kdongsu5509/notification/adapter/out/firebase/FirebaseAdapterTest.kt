package com.kdongsu5509.imhere.notification.adapter.out.firebase

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FirebaseAdapterTest {
    @Mock
    private lateinit var firebaseMessaging: FirebaseMessaging

    @InjectMocks
    private lateinit var firebaseAdapter: FirebaseAdapter

    @Test
    @DisplayName("fcm 토큰을 받으면 메시지를 잘 전송한다")
    fun send_success() {
        //given
        val testFcmToken = "testFcmToken"

        //when
        firebaseAdapter.send(testFcmToken)

        //then
        verify(firebaseMessaging, times(1)).send(any(Message::class.java))
    }
}