package com.kdongsu5509.support.exception

import com.kdongsu5509.support.exception.type.*

/**
 * [ImHereBaseErrorCode]를 바탕으로 적절한 시맨틱 예외를 던집니다.
 * 'imhereErrorCode', 'errorMessage', 'contextData'를 일관되게 처리합니다.
 */
fun ImHereBaseErrorCode.throwIt(
    contextData: Map<String, Any?> = emptyMap(),
    customMessage: String? = null,
    cause: Throwable? = null
): Nothing {
    val finalMessage = customMessage ?: this.errorMessage

    throw when (this.httpStatus.value()) {
        400 -> InvalidInputException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        401 -> UnauthorizedException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        403 -> ForbiddenException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        404 -> NotFoundException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        409 -> ConflictException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        500 -> InternalServerException(
            message = finalMessage,
            contextData = contextData,
            cause = cause,
            errorCode = this
        )

        else -> ImHereBaseException(
            errorCode = this,
            overrideMessage = finalMessage,
            contextData = contextData,
            cause = cause
        )
    }
}
