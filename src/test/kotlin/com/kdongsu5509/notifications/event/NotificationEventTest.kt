package com.kdongsu5509.notifications.event

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class NotificationEventTest {
    private val senderId = UUID.randomUUID()

    @Test
    @DisplayName("SMS는 요청자가 쓴 본문을 사용한다")
    fun sms_uses_the_requested_body() {
        val event = event(NotificationMethod.SMS, mapOf(NotificationCommand.BODY_KEY to "지금 출발합니다"))

        assertThat(event.bodyOverride()).isEqualTo("지금 출발합니다")
    }

    @Test
    @DisplayName("SMS 본문이 비어 있으면 접수를 거부한다")
    fun sms_without_body_is_rejected() {
        val event = event(NotificationMethod.SMS, mapOf(NotificationCommand.BODY_KEY to "   "))

        assertThatThrownBy { event.bodyOverride() }
            .isInstanceOf(InvalidInputException::class.java)
    }

    @Test
    @DisplayName("FCM은 템플릿 본문을 쓰므로 본문을 덮어쓰지 않는다")
    fun fcm_keeps_the_template_body() {
        val event = event(NotificationMethod.FCM, emptyMap())

        assertThat(event.bodyOverride()).isNull()
    }

    @Test
    @DisplayName("전화번호를 대상으로 삼은 알림은 사용자로 풀리지 않는다")
    fun phone_number_target_has_no_user_id() {
        val phoneTarget = event(NotificationMethod.SMS, mapOf(NotificationCommand.BODY_KEY to "본문"))
            .copy(targetIdentifier = "01000000000")

        assertThat(phoneTarget.targetUserId).isNull()
    }

    @Test
    @DisplayName("사용자를 대상으로 삼은 알림은 사용자 식별자로 풀린다")
    fun user_target_resolves_to_user_id() {
        val receiverId = UUID.randomUUID()
        val event = event(NotificationMethod.FCM, emptyMap()).copy(targetIdentifier = receiverId.toString())

        assertThat(event.targetUserId).isEqualTo(receiverId)
    }

    private fun event(method: NotificationMethod, extraData: Map<String, String>): NotificationEvent =
        NotificationEvent(
            senderNickname = "보낸이",
            senderId = senderId,
            notificationMethod = method,
            targetIdentifier = UUID.randomUUID().toString(),
            type = NotificationType.ARRIVAL,
            extraData = extraData,
        )
}
