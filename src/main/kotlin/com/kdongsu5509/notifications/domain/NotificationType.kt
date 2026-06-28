package com.kdongsu5509.notifications.domain

import com.kdongsu5509.support.exception.type.InvalidInputException

/**
 * 알림 종류 도메인 모델.
 *
 * 각 종류가 자신의 표현(제목/본문)·딥링크 경로·푸시 정책(채널/우선순위)을 직접 안다.
 * 새 종류 추가 시 이 enum 한 곳만 수정하면 된다(메시지/채널/경로 switch 분산 제거).
 *
 * 외부(RabbitMQ JSON, FCM data["type"])와는 [name]으로 직렬화되므로 상수 이름 변경은 호환성에 영향을 준다.
 * 푸시 우선순위는 Firebase SDK 타입에 의존하지 않도록 도메인 enum [AndroidPushPriority]로 표현하고,
 * 실제 `AndroidConfig.Priority` 변환은 어댑터(FirebaseAdapter)가 담당한다.
 *
 * 본문 템플릿은 발송자 닉네임과 추가 데이터([extraData])를 받는다. 도착/출발 알림은 [PLACE_NAME_KEY]로
 * 전달된 장소명을 본문에 포함하며, 누락 시 [InvalidInputException]을 던진다(클라이언트가 반드시 전달해야 함).
 */
enum class NotificationType(
    val appPath: String,
    val titleText: String,
    private val bodyTemplate: (senderNickname: String, extraData: Map<String, String>) -> String,
    val androidChannelId: String,
    val pushPriority: AndroidPushPriority,
) {
    FRIEND_REQUEST_RECEIVED(
        appPath = "/friend/requests",
        titleText = "새로운 친구 요청",
        bodyTemplate = { sender, _ -> "${sender}님이 친구 요청을 보냈습니다." },
        androidChannelId = "fcm_high_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    FRIEND_REQUEST_ACCEPTED(
        appPath = "/friend",
        titleText = "친구 요청 수락",
        bodyTemplate = { sender, _ -> "${sender}님이 친구 요청을 수락했습니다." },
        androidChannelId = "fcm_normal_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    LOCATION_SHARE_RECEIVED(
        appPath = "/record/notifications",
        titleText = "위치 공유 알림",
        bodyTemplate = { sender, _ -> "${sender}님이 위치를 공유했습니다." },
        androidChannelId = "fcm_high_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    LOCATION_TARGET(
        appPath = "/record/notifications",
        titleText = "위치 공유 알림",
        bodyTemplate = { sender, _ -> "${sender}님이 위치를 공유했습니다." },
        androidChannelId = "fcm_high_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    ARRIVAL(
        appPath = "/record/notifications",
        titleText = "도착 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에 도착했습니다." },
        androidChannelId = "fcm_critical_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    DEPARTURE(
        appPath = "/record/notifications",
        titleText = "출발 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에서 출발했습니다." },
        androidChannelId = "fcm_critical_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    ARRIVAL_CONFIRMATION(
        appPath = "/record/notifications",
        titleText = "도착 안내",
        bodyTemplate = { sender, data -> "${sender}님이 ${requirePlaceName(data)}에 도착했습니다." },
        androidChannelId = "fcm_critical_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    TERMS_UPDATE_NOTICE(
        appPath = "/terms-detail/{termId}",
        titleText = "서비스 공지사항",
        bodyTemplate = { _, _ -> "이용약관이 업데이트되었습니다. 내용을 확인해 주세요." },
        androidChannelId = "fcm_silent_channel",
        pushPriority = AndroidPushPriority.NORMAL,
    ),
    DELIVERY_RESULT_NOTICE(
        appPath = "/record/send-history",
        titleText = "발송 결과 알림",
        bodyTemplate = { _, _ -> "요청하신 발송 작업이 완료되었습니다." },
        androidChannelId = "fcm_silent_channel",
        pushPriority = AndroidPushPriority.NORMAL,
    ),
    DELIVERY_FAILED_NOTICE(
        appPath = "/record/send-history",
        titleText = "발송 실패 알림",
        bodyTemplate = { _, _ -> "요청하신 발송 작업이 실패했습니다. 잠시 후 다시 시도해 주세요." },
        androidChannelId = "fcm_normal_channel",
        pushPriority = AndroidPushPriority.HIGH,
    ),
    ;

    /**
     * 발송자 닉네임과 추가 데이터로 이 종류의 알림 본문을 만든다.
     * 도착/출발 알림은 [extraData]의 [PLACE_NAME_KEY]를 사용한다.
     */
    fun bodyText(senderNickname: String, extraData: Map<String, String> = emptyMap()): String =
        bodyTemplate(senderNickname, extraData)

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
            LOCATION_SHARE_RECEIVED,
            LOCATION_TARGET,
            ARRIVAL,
            DEPARTURE,
            ARRIVAL_CONFIRMATION,
        )

        fun fromName(name: String?): NotificationType? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }

        private fun requirePlaceName(extraData: Map<String, String>): String =
            extraData[PLACE_NAME_KEY]?.takeIf { it.isNotBlank() }
                ?: throw InvalidInputException("도착/출발 알림에는 장소명($PLACE_NAME_KEY)이 필요합니다.")
    }
}

/** Firebase SDK에 독립적인 도메인 푸시 우선순위. 어댑터에서 AndroidConfig.Priority로 변환된다. */
enum class AndroidPushPriority { HIGH, NORMAL }
