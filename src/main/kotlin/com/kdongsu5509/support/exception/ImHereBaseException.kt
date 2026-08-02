package com.kdongsu5509.support.exception

open class ImHereBaseException(
    val errorCode: ImHereBaseErrorCode,
    val overrideMessage: String? = null,
    val contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(overrideMessage ?: errorCode.errorMessage, cause)
