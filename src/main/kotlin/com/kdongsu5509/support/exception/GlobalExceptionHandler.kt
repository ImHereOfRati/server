package com.kdongsu5509.support.exception

import com.kdongsu5509.support.common.dto.APIResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(BusinessException::class)
    protected fun handleBusinessException(e: BusinessException): ResponseEntity<APIResponse<ErrorResponse>> {
        val errorCode = e.errorCode
        logger.warn("Business exception occurred: code={}, message={}", errorCode.code, e.message ?: errorCode.message)

        val errorResponse = ErrorResponse(errorCode.code, e.message ?: errorCode.message)
        return ResponseEntity
            .status(errorCode.status)
            .body(
                APIResponse.fail(
                    errorCode.status.value(),
                    errorCode.status.name,
                    errorResponse
                )
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<APIResponse<ErrorResponse>> {
        logger.warn("MethodArgumentNotValidException occurred: {}", e.message)
        val errorResponse =
            ErrorResponse(GlobalErrorCode.INVALID_INPUT_VALUE.code, GlobalErrorCode.INVALID_INPUT_VALUE.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                APIResponse.fail(
                    HttpStatus.BAD_REQUEST.value(),
                    HttpStatus.BAD_REQUEST.name,
                    errorResponse
                )
            )
    }

    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<APIResponse<ErrorResponse>> {
        logger.error("Unexpected exception occurred: ", e)
        val errorResponse = ErrorResponse(GlobalErrorCode.UNKNOWN_ERROR.code, GlobalErrorCode.UNKNOWN_ERROR.message)
        return ResponseEntity
            .status(GlobalErrorCode.UNKNOWN_ERROR.status)
            .body(
                APIResponse.fail(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.name,
                    errorResponse
                )
            )
    }
}
