package com.kdongsu5509.notifications.domain

enum class AndroidPushPriority { HIGH, NORMAL }

enum class ApnsPushPriority(val headerValue: String) {
    IMMEDIATE("10"),
    THROTTLED("5"),
}

enum class IosInterruptionLevel(val value: String) {
    PASSIVE("passive"),
    ACTIVE("active"),
    TIME_SENSITIVE("time-sensitive"),
}

enum class PushChannel(
    val androidChannelId: String,
    val androidPriority: AndroidPushPriority,
    val apnsPriority: ApnsPushPriority,
    val interruptionLevel: IosInterruptionLevel,
    val sound: String?,
) {
    CRITICAL(
        androidChannelId = "fcm_critical_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.TIME_SENSITIVE,
        sound = DEFAULT_SOUND,
    ),

    HIGH(
        androidChannelId = "fcm_high_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.ACTIVE,
        sound = DEFAULT_SOUND,
    ),

    NORMAL(
        androidChannelId = "fcm_normal_channel",
        androidPriority = AndroidPushPriority.HIGH,
        apnsPriority = ApnsPushPriority.IMMEDIATE,
        interruptionLevel = IosInterruptionLevel.ACTIVE,
        sound = DEFAULT_SOUND,
    ),

    SILENT(
        androidChannelId = "fcm_silent_channel",
        androidPriority = AndroidPushPriority.NORMAL,
        apnsPriority = ApnsPushPriority.THROTTLED,
        interruptionLevel = IosInterruptionLevel.PASSIVE,
        sound = null,
    ),
    ;

    val isSilent: Boolean
        get() = sound == null

    companion object {
        fun of(type: NotificationType): PushChannel = when (type) {
            NotificationType.ARRIVAL,
            NotificationType.DEPARTURE -> CRITICAL

            NotificationType.FRIEND_REQUEST_RECEIVED,
            NotificationType.LOCATION_TARGET -> HIGH

            NotificationType.FRIEND_REQUEST_ACCEPTED,
            NotificationType.DELIVERY_FAILED_NOTICE -> NORMAL

            NotificationType.TERMS_UPDATE_NOTICE,
            NotificationType.DELIVERY_RESULT_NOTICE -> SILENT
        }
    }
}

private const val DEFAULT_SOUND = "default"
