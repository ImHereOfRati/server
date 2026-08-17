package com.kdongsu5509.notifications.exception

import com.kdongsu5509.support.exception.ImHereBaseException

class UnregisteredTokenException(
    message: String = "등록 해제된 FCM 토큰입니다.",
    cause: Throwable? = null,
) : ImHereBaseException(
    errorCode = NotificationException.FCM_TOKEN_UNREGISTERED,
    overrideMessage = message,
    cause = cause,
)
