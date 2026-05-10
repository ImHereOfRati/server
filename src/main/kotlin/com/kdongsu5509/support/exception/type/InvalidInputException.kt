package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason

class InvalidInputException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : BaseException(ErrorReason.INVALID_INPUT, message, metadata, cause)
