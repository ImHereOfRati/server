package com.kdongsu5509.support.exception.handler

import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.support.response.APIResponseBody
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@Order(99)
class GlobalFallbackExceptionHandler : AbstractExceptionHandler() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        log.error("Unexpected error occurred: ", e)
        return createErrorResponse(ErrorReason.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.")
    }
}
