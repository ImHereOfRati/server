package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason

class ConflictException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : BaseException(ErrorReason.CONFLICT, message, metadata, cause)
