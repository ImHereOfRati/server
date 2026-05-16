package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException

class InternalServerException(
    message: String? = null,
    contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
    errorCode: ImHereBaseErrorCode = CommonErrorCode.INTERNAL_SERVER_ERROR
) : ImHereBaseException(errorCode, message, contextData, cause)
