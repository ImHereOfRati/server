package com.kdongsu5509.support.exception.handler

import com.kdongsu5509.support.exception.BaseException
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.support.response.APIResponseBody
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(1)
class BaseExceptionHandler(
    private val discordUserErrorNotifier: DiscordUserErrorNotifier
) : AbstractExceptionHandler() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(BaseException::class)
    fun handleBaseException(
        e: BaseException,
        request: HttpServletRequest
    ): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        log.warn("[{}] {} (metadata: {})", e.errorCategory, e.message, e.metadata)

        if (e.errorCategory.isCritical()) {
            discordUserErrorNotifier.notifyUserError(request, e.errorCategory.name, e.message)
        }

        return createErrorResponse(e.errorCategory, e.message, e.metadata)
    }
}
