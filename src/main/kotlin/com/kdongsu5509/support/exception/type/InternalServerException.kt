package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason

class InternalServerException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : BaseException(ErrorReason.INTERNAL_SERVER_ERROR, message, metadata, cause)

