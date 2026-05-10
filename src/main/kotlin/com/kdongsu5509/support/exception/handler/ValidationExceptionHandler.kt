package com.kdongsu5509.support.exception.handler

import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.support.response.APIResponseBody
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
@Order(2)
class ValidationExceptionHandler : AbstractExceptionHandler() {
    private val log = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException
    ): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        val message = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("Validation failed: {}", message)
        return createErrorResponse(ErrorReason.INVALID_INPUT, "입력값이 올바르지 않습니다: $message")
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, MissingServletRequestParameterException::class)
    fun handleBadRequestExceptions(
        ex: Exception
    ): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        log.warn("Bad request: {} - {}", ex.javaClass.simpleName, ex.message)
        return createErrorResponse(ErrorReason.INVALID_INPUT, "잘못된 요청 형식입니다: ${ex.message}")
    }
}
