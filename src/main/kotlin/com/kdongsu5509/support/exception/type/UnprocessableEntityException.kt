package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException

class UnprocessableEntityException(
    message: String? = null,
    contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : ImHereBaseException(CommonErrorCode.UNPROCESSABLE_ENTITY, message, contextData, cause)
