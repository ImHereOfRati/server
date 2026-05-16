package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException

class InvalidInputException(
    message: String? = null,
    contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
    errorCode: ImHereBaseErrorCode = CommonErrorCode.INVALID_INPUT
) : ImHereBaseException(errorCode, message, contextData, cause)
