package com.kdongsu5509.notifications.exception

import com.kdongsu5509.support.exception.ImHereBaseException

class RetryableFcmException(
    message: String = NotificationException.FCM_RETRYABLE_ERROR.errorMessage,
    cause: Throwable? = null,
) : ImHereBaseException(
    errorCode = NotificationException.FCM_RETRYABLE_ERROR,
    overrideMessage = message,
    cause = cause,
)
