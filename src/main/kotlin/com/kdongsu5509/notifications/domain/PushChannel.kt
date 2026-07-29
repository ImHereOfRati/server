package com.kdongsu5509.notifications.domain

/** Firebase SDK에 독립적인 도메인 푸시 우선순위(Android). 어댑터에서 `AndroidConfig.Priority`로 변환된다. */
enum class AndroidPushPriority { HIGH, NORMAL }

/**
 * APNs `apns-priority` 헤더 값.
 *
 * [IMMEDIATE]는 즉시 전달하고, [THROTTLED]는 기기 전력 상황에 맞춰 미뤄 전달한다.
 */
enum class ApnsPushPriority(val headerValue: String) {
    IMMEDIATE("10"),
    THROTTLED("5"),
}

/**
 * iOS 알림 방해 수준(`aps.interruption-level`).
 *
 * [TIME_SENSITIVE]는 집중 모드를 뚫고 뜨고, [PASSIVE]는 알림 목록에만 조용히 쌓인다.
 */
enum class IosInterruptionLevel(val value: String) {
    PASSIVE("passive"),
    ACTIVE("active"),
    TIME_SENSITIVE("time-sensitive"),
}

/**
 * 푸시 전달 정책. Android 채널·우선순위와 iOS APNs 정책을 한 쌍으로 묶는다.
 *
 * 예전에는 [NotificationType]이 `androidChannelId`와 `pushPriority`를 개별 필드로 들고 있었고 iOS 정책은 아예 없었다.
 * 그 상태로 iOS를 더하면 알림 종류 9개마다 필드를 네 개씩 늘려야 했다.
 * 실제로 두 필드의 조합이 정확히 네 가지뿐이었으므로, 조합 자체를 한 단계 끌어올려 정책 그룹으로 만든다.
 * 알림 종류가 늘어도 이 넷 중 하나를 고르면 되고, 새 플랫폼이 생겨도 여기만 손보면 된다.
 *
 * 값은 도메인 표현이며 Firebase SDK 타입으로의 변환은 어댑터가 맡는다.
 */
enum class PushChannel(
    val androidChannelId: String,
    val androidPriority: AndroidPushPriority,
    val apnsPriority: ApnsPushPriority,
    val interruptionLevel: IosInterruptionLevel,
    /** null이면 무음으로 보낸다. */
    val sound: String?,
) {
    /** 도착·출발처럼 놓치면 안 되는 알림. 집중 모드를 뚫는다. */
    CRITICAL(
        androidChannelId = "fcm_critical_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.TIME_SENSITIVE,
        sound = DEFAULT_SOUND,
    ),

    /** 친구 요청·위치 공유처럼 바로 알려야 하는 알림. */
    HIGH(
        androidChannelId = "fcm_high_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.ACTIVE,
        sound = DEFAULT_SOUND,
    ),

    /** 친구 수락·발송 실패처럼 알리되 급하지는 않은 알림. */
    NORMAL(
        androidChannelId = "fcm_normal_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.ACTIVE,
        sound = DEFAULT_SOUND,
    ),

    /** 공지·발송 결과처럼 방해하지 않고 쌓아두면 되는 알림. */
    SILENT(
        androidChannelId = "fcm_silent_channel",
        androidPriority = AndroidPushPriority.NORMAL,
        apnsPriority = ApnsPushPriority.THROTTLED,
        interruptionLevel = IosInterruptionLevel.PASSIVE,
        sound = null,
    ),
    ;

    /** 소리 없이 도착하는 채널인가. */
    val isSilent: Boolean
        get() = sound == null
}

/** APNs 기본 알림음. */
private const val DEFAULT_SOUND = "default"
