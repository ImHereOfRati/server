package com.kdongsu5509.support.exception

/**
 * 모든 비즈니스 예외의 기본 클래스
 */
open class ImHereBaseException(
    val errorCode: ImHereBaseErrorCode,
    val overrideMessage: String? = null,
    val contextData: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null
) : RuntimeException(overrideMessage ?: errorCode.errorMessage, cause)
