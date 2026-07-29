package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.domain.NotificationType.Companion.PLACE_NAME_KEY
import com.kdongsu5509.support.exception.type.InvalidInputException

/**
 * 알림 종류 도메인 모델.
 *
 * 각 종류가 자신의 표현(제목/본문)·딥링크 경로·푸시 정책(채널/우선순위)을 직접 안다.
 * 새 종류 추가 시 이 enum 한 곳만 수정하면 된다(메시지/채널/경로 switch 분산 제거).
 *
 * 외부(발행 이벤트, FCM data["type"])와는 [name]으로 직렬화되므로 상수 이름 변경은 호환성에 영향을 준다.
 * 플랫폼별 전달 정책(Android 채널·우선순위, iOS APNs 우선순위·방해 수준·알림음)은 [PushChannel]이 한 쌍으로 소유하고,
 * 실제 Firebase SDK 타입으로의 변환은 어댑터(FirebaseAdapter)가 담당한다.
 *
 * 본문 템플릿은 발송자 닉네임과 추가 데이터([extraData])를 받는다. 도착/출발 알림은 [PLACE_NAME_KEY]로
 * 전달된 장소명을 본문에 포함하며, 누락 시 [InvalidInputException]을 던진다(클라이언트가 반드시 전달해야 함).
 */
enum class NotificationType(
    val appPath: String,
    val titleText: String,
    private val bodyTemplate: (senderNickname: String, extraData: Map<String, String>) -> String,
    val channel: PushChannel,
) {
    FRIEND_REQUEST_RECEIVED(
        appPath = "/friend/requests",
        titleText = "새로운 친구 요청",
        bodyTemplate = { sender, _ -> "${sender}님이 친구 요청을 보냈습니다." },
        channel = PushChannel.HIGH,
    ),
    FRIEND_REQUEST_ACCEPTED(
        appPath = "/friend",
        titleText = "친구 요청 수락",
        bodyTemplate = { sender, _ -> "${sender}님이 친구 요청을 수락했습니다." },
        channel = PushChannel.NORMAL,
    ),
    LOCATION_TARGET(
        appPath = "/record/notifications",
        titleText = "위치 공유 대상자 알림",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에 도착/출발하면 알람을 다시 보내드릴게요." },
        channel = PushChannel.HIGH,
    ),
    ARRIVAL(
        appPath = "/record/notifications",
        titleText = "도착 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에 도착했습니다." },
        channel = PushChannel.CRITICAL,
    ),
    DEPARTURE(
        appPath = "/record/notifications",
        titleText = "출발 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에서 출발했습니다." },
        channel = PushChannel.CRITICAL,
    ),
    ARRIVAL_CONFIRMATION(
        appPath = "/record/notifications",
        titleText = "도착 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에 도착했습니다." },
        channel = PushChannel.CRITICAL,
    ),
    TERMS_UPDATE_NOTICE(
        appPath = "/terms-detail/{termId}",
        titleText = "서비스 공지사항",
        bodyTemplate = { _, _ -> "이용약관이 업데이트되었습니다. 내용을 확인해 주세요." },
        channel = PushChannel.SILENT,
    ),
    DELIVERY_RESULT_NOTICE(
        appPath = "/record/send-history",
        titleText = "발송 결과 알림",
        bodyTemplate = { _, _ -> "요청하신 발송 작업이 완료되었습니다." },
        channel = PushChannel.SILENT,
    ),
    DELIVERY_FAILED_NOTICE(
        appPath = "/record/send-history",
        titleText = "발송 실패 알림",
        bodyTemplate = { _, _ -> "요청하신 발송 작업이 실패했습니다. 잠시 후 다시 시도해 주세요." },
        channel = PushChannel.NORMAL,
    ), ;

    /** Android 알림 채널 ID. 정책의 주인은 [channel]이고 여기서는 꺼내 쓰기만 한다. */
    val androidChannelId: String
        get() = channel.androidChannelId

    /** Android 푸시 우선순위. 정책의 주인은 [channel]이다. */
    val pushPriority: AndroidPushPriority
        get() = channel.androidPriority

    /**
     * 발송 결과(성공/실패)를 알리는 메타 타입인지 여부.
     *
     * 메타 타입은 발송 요청자에게 보내는 수신증(delivery receipt)이므로, 이 알림의 발송 결과로
     * 또 다른 수신증을 발행하면 무한 재귀 발행이 된다. 컨슈머/실패통지기는 이 질의로 후속 발행을 건너뛴다.
     */
    val isMeta: Boolean
        get() = this == DELIVERY_RESULT_NOTICE || this == DELIVERY_FAILED_NOTICE

    /**
     * 발송자 닉네임과 추가 데이터로 이 종류의 알림 본문을 만든다.
     * 도착/출발 알림은 [extraData]의 [PLACE_NAME_KEY]를 사용한다.
     */
    fun bodyText(senderNickname: String, extraData: Map<String, String> = emptyMap()): String =
        bodyTemplate(senderNickname, extraData)

    /**
     * 발송자와 추가 데이터로 이 종류의 알림을 하나의 [RenderedNotification]으로 조립한다.
     * 제목/본문/딥링크 경로와 FCM data 맵(`type`/`path`/`senderNickname`/`senderEmail` 포함) 조립을 한곳에 응집한다.
     */
    fun render(
        senderNickname: String,
        senderEmail: String,
        extraData: Map<String, String> = emptyMap(),
    ): RenderedNotification {
        val path = resolvePath(extraData)
        val data = extraData + mapOf(
            "senderNickname" to senderNickname,
            "senderEmail" to senderEmail,
            "type" to name,
            "path" to path,
        )
        return RenderedNotification(
            type = this,
            senderNickname = senderNickname,
            title = titleText,
            body = bodyText(senderNickname, extraData),
            path = path,
            data = data,
        )
    }

    /**
     * [appPath] 템플릿의 `{key}` 자리표시자를 [extraData] 값으로 치환한 딥링크 경로를 만든다.
     * 필수 키가 없으면 [InvalidInputException]을 던진다.
     */
    fun resolvePath(extraData: Map<String, String>): String =
        PLACEHOLDER_REGEX.replace(appPath) { match ->
            val key = match.groupValues[1]
            extraData[key] ?: throw InvalidInputException("알림 경로 생성 중 필수 데이터($key)가 누락되었습니다.")
        }

    companion object {
        /** 도착/출발 알림 본문에 들어갈 장소명을 담는 extraData 키. 클라이언트가 전달해야 한다. */
        const val PLACE_NAME_KEY = "placeName"

        private val PLACEHOLDER_REGEX = Regex("\\{(\\w+)\\}")

        val CLIENT_ALLOWED = setOf(
            LOCATION_TARGET,
            ARRIVAL,
            DEPARTURE,
            ARRIVAL_CONFIRMATION,
        )

        private fun requirePlaceName(extraData: Map<String, String>): String =
            extraData[PLACE_NAME_KEY]?.takeIf { it.isNotBlank() }
                ?: throw InvalidInputException("도착/출발 알림에는 장소명($PLACE_NAME_KEY)이 필요합니다.")
    }
}
