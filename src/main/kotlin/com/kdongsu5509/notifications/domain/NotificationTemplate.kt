package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException

object NotificationTemplate {

    const val PLACE_NAME_KEY = "placeName"

    fun render(
        type: NotificationType,
        senderAlias: String,
        extraData: Map<String, String> = emptyMap(),
    ): RenderedNotification {
        val message = messageOf(type, senderAlias, extraData)
        val payloadData = payloadDataOf(type, extraData)
        return RenderedNotification(
            type = type,
            senderAlias = senderAlias,
            title = message.title,
            body = message.body,
            channel = PushChannel.of(type),
            data = payloadData + mapOf(
                "type" to type.name,
                "senderAlias" to senderAlias,
            ),
        )
    }

    private fun messageOf(
        type: NotificationType,
        senderAlias: String,
        extraData: Map<String, String>,
    ): Message = when (type) {
        NotificationType.FRIEND_REQUEST_RECEIVED -> Message(
            title = "새로운 친구 요청",
            body = "${senderAlias}님이 친구 요청을 보냈습니다.",
        )

        NotificationType.FRIEND_REQUEST_ACCEPTED -> Message(
            title = "친구 요청 수락",
            body = "${senderAlias}님이 친구 요청을 수락했습니다.",
        )

        NotificationType.LOCATION_TARGET -> Message(
            title = "위치 공유 대상자 알림",
            body = "${senderAlias}님이 ${placeName(extraData)}에 도착/출발하면 알람을 다시 보내드릴게요.",
        )

        NotificationType.ARRIVAL -> Message(
            title = "도착 안내",
            body = "${senderAlias}님이 ${placeName(extraData)}에 도착했습니다.",
        )

        NotificationType.DEPARTURE -> Message(
            title = "출발 안내",
            body = "${senderAlias}님이 ${placeName(extraData)}에서 출발했습니다.",
        )

        NotificationType.TERMS_UPDATE_NOTICE -> Message(
            title = "서비스 공지사항",
            body = "이용약관이 업데이트되었습니다. 내용을 확인해 주세요.",
        )

        NotificationType.DELIVERY_RESULT_NOTICE -> Message(
            title = "발송 결과 알림",
            body = "요청하신 발송 작업이 완료되었습니다.",
        )

        NotificationType.DELIVERY_FAILED_NOTICE -> Message(
            title = "발송 실패 알림",
            body = "요청하신 발송 작업이 실패했습니다. 잠시 후 다시 시도해 주세요.",
        )
    }

    private fun placeName(extraData: Map<String, String>): String =
        extraData[PLACE_NAME_KEY]?.takeIf { it.isNotBlank() }
            ?: throw InvalidInputException("도착/출발 알림에는 장소명($PLACE_NAME_KEY)이 필요합니다.")

    /**
     * FCM data is a wire contract. Do not forward request-only fields such as
     * body/path or legacy sender fields to the device.
     */
    private fun payloadDataOf(
        type: NotificationType,
        extraData: Map<String, String>,
    ): Map<String, String> = when (type) {
        NotificationType.LOCATION_TARGET,
        NotificationType.ARRIVAL,
        NotificationType.DEPARTURE -> mapOf(PLACE_NAME_KEY to placeName(extraData))

        else -> emptyMap()
    }

    private data class Message(val title: String, val body: String)
}
