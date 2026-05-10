package com.kdongsu5509.support.exception.type

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.exception.ErrorReason

class InfraFailureException(
    message: String,
    metadata: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : BaseException(ErrorReason.INFRA_FAILURE, message, metadata, cause)

