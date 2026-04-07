package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.out.DeleteTokenPort
import com.kdongsu5509.notifications.application.port.out.FindTokenPort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FCMMessageTitle
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FCMErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class FCMNotificationServiceTest {

    @Mock
    private lateinit var findTokenPort: FindTokenPort

    @Mock
    private lateinit var deleteTokenPort: DeleteTokenPort

    @Mock
    private lateinit var firebasePort: FirebasePort

    @InjectMocks
    private lateinit var fcmNotificationService: FCMNotificationService

    companion object {
        const val SENDER_NICKNAME = "sender-nick"
        const val SENDER_EMAIL = "sender@example.com"
        const val RECEIVER_EMAIL = "test@example.com"
        const val BODY = "테스트 알림 내용"
        const val FCM_TOKEN = "sample-fcm-token"
    }

    @Test
    @DisplayName("알림 전송 성공")
    fun send_success() {
        // given
        val fcmToken = FcmToken(1L, RECEIVER_EMAIL, FCM_TOKEN, DeviceType.AOS, LocalDateTime.now())
        `when`(findTokenPort.findByUserEmail(RECEIVER_EMAIL)).thenReturn(fcmToken)

        // when
        fcmNotificationService.send(SENDER_NICKNAME, SENDER_EMAIL, RECEIVER_EMAIL, NotificationType.FRIEND_REQUEST.name, BODY)

        // then
        val expectedData = mapOf("senderNickname" to SENDER_NICKNAME, "senderEmail" to SENDER_EMAIL)
        verify(firebasePort).send(FCM_TOKEN, FCMMessageTitle.FRIEND_REQUEST, BODY, expectedData)
    }

    @Test
    @DisplayName("알림 전송 시 토큰을 찾을 수 없으면 예외 발생")
    fun send_token_not_found() {
        // given
        `when`(findTokenPort.findByUserEmail(RECEIVER_EMAIL)).thenReturn(null)

        // when & then
        val exception = assertThrows<BusinessException> {
            fcmNotificationService.send(SENDER_NICKNAME, SENDER_EMAIL, RECEIVER_EMAIL, NotificationType.FRIEND_REQUEST.name, BODY)
        }
        assertThat(exception.errorCode).isEqualTo(FCMErrorCode.FCM_TOKEN_NOT_FOUND)
    }

    @Test
    @DisplayName("FCM 토큰이 등록 해제된 경우 삭제 처리")
    fun send_token_unregistered_then_delete() {
        // given
        val fcmToken = FcmToken(1L, RECEIVER_EMAIL, FCM_TOKEN, DeviceType.AOS, LocalDateTime.now())
        val data = mapOf("senderNickname" to SENDER_NICKNAME, "senderEmail" to SENDER_EMAIL)
        `when`(findTokenPort.findByUserEmail(RECEIVER_EMAIL)).thenReturn(fcmToken)
        `doThrow`(BusinessException(FCMErrorCode.FCM_TOKEN_UNREGISTERED))
            .`when`(firebasePort).send(FCM_TOKEN, FCMMessageTitle.FRIEND_REQUEST, BODY, data)

        // when
        fcmNotificationService.send(SENDER_NICKNAME, SENDER_EMAIL, RECEIVER_EMAIL, NotificationType.FRIEND_REQUEST.name, BODY)

        // then
        verify(deleteTokenPort).deleteById(1L)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 친구 요청")
    fun convertTypeToMessageTitle_friend_request() {
        // when
        val title = fcmNotificationService.convertTypeToMessageTitle(NotificationType.FRIEND_REQUEST.name)

        // then
        assertThat(title).isEqualTo(FCMMessageTitle.FRIEND_REQUEST)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 약관 업데이트")
    fun convertTypeToMessageTitle_terms_update() {
        // when
        val title = fcmNotificationService.convertTypeToMessageTitle(NotificationType.TERMS_UPDATE.name)

        // then
        assertThat(title).isEqualTo(FCMMessageTitle.DEFAULT_NOTICE)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 위치 알림 대상자 등록")
    fun convertTypeToMessageTitle_location_share_recipient() {
        // when
        val title = fcmNotificationService.convertTypeToMessageTitle(NotificationType.LOCATION_SHARE_RECIPIENT.name)

        // then
        assertThat(title).isEqualTo(FCMMessageTitle.LOCATION_SHARE_RECIPIENT)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 도착 알림")
    fun convertTypeToMessageTitle_arrival_confirmation() {
        // when
        val title = fcmNotificationService.convertTypeToMessageTitle(NotificationType.ARRIVAL_CONFIRMATION.name)

        // then
        assertThat(title).isEqualTo(FCMMessageTitle.ARRIVAL_CONFIRMATION)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 전송 결과 안내")
    fun convertTypeToMessageTitle_delivery_result_notice() {
        // when
        val title = fcmNotificationService.convertTypeToMessageTitle(NotificationType.DELIVERY_RESULT_NOTICE.name)

        // then
        assertThat(title).isEqualTo(FCMMessageTitle.DELIVERY_RESULT_NOTICE)
    }

    @Test
    @DisplayName("알림 타입에 따른 메시지 제목 변환 - 알 수 없는 타입 시 예외 발생")
    fun convertTypeToMessageTitle_invalid_type() {
        // when & then
        val exception = assertThrows<BusinessException> {
            fcmNotificationService.convertTypeToMessageTitle("INVALID_TYPE")
        }
        assertThat(exception.errorCode).isEqualTo(FCMErrorCode.FCM_INVALID_ARGUMENT)
    }
}
