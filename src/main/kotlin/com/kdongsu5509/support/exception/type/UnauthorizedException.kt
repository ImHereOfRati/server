package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException

class UnauthorizedException(
    message: String? = null,
    contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
    errorCode: ImHereBaseErrorCode = CommonErrorCode.UNAUTHORIZED
) : ImHereBaseException(errorCode, message, contextData, cause)
