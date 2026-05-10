package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason

class NotFoundException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : BaseException(ErrorReason.NOT_FOUND, message, metadata, cause)
