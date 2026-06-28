package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.adapter.`in`.web.dto.validation.ValidTargetId
import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@ValidTargetId
data class MultiNotificationRequest(
    @field:NotNull(message = "발송 대상 타입을 입력해 주세요.")
    val notificationMethod: NotificationMethod,

    @field:NotEmpty(message = "알림을 수신할 대상(이메일 또는 전화번호)을 1명 이상 입력해 주세요.")
    val targetIds: List<String>,

    @field:NotNull(message = "알림 템플릿 타입을 선택해 주세요.")
    val type: NotificationType,

    val extraData: Map<String, String> = emptyMap()
) {
    @AssertTrue(message = "클라이언트가 직접 발송할 수 없는 알림 타입입니다.")
    fun isClientAllowedType(): Boolean = type in NotificationType.CLIENT_ALLOWED

    fun toCommand(senderNickname: String, senderEmail: String) = MultipleNotificationCommand(
        senderNickname = senderNickname,
        senderEmail = senderEmail,
        notificationMethod = notificationMethod,
        targetIdentifiers = targetIds,
        type = type.name,
        extraData = extraData
    )
}
