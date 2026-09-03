package com.kdongsu5509.support.handler

import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toFailResponse
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.external.UserErrorAlertNotifier
import com.kdongsu5509.support.logger.logger
import com.kdongsu5509.support.logger.SensitiveMasker
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice(basePackages = ["com.kdongsu5509"])
class GlobalExceptionHandler(
    private val userErrorAlertNotifier: UserErrorAlertNotifier
) {
    private val log = logger()

    @ExceptionHandler(ImHereBaseException::class)
    fun handleBaseException(
        e: ImHereBaseException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        val errorCode = e.errorCode
        log.warn("[{}] {} (context: {})", errorCode.imhereErrorCode, e.message, maskedContext(e.contextData))

        userErrorAlertNotifier.notifyUserError(
            request,
            errorCode.imhereErrorCode,
            e.message ?: errorCode.errorMessage
        )

        val headers = HttpHeaders()
        (e.contextData["retryAfterSeconds"] as? Number)?.let {
            if (e.errorCode == NotificationException.SMS_DAILY_RECIPIENT_LIMIT) {
                headers.set("Retry-After", it.toLong().toString())
            }
        }

        return e.contextData.toFailResponse(
            status = errorCode.httpStatus,
            imhereErrorCode = errorCode.imhereErrorCode,
            errorMessage = e.message,
            headers = headers
        )
    }

    // --- 400 Bad Request ---

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        e: MethodArgumentNotValidException
    ): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        val message = e.bindingResult.fieldErrors.joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("입력값 검증 실패: {}", message)

        return null.toFailResponse(
            status = CommonErrorCode.INVALID_INPUT.httpStatus,
            imhereErrorCode = CommonErrorCode.INVALID_INPUT.imhereErrorCode,
            errorMessage = "입력값이 올바르지 않습니다: $message"
        )
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        ConstraintViolationException::class,
        HandlerMethodValidationException::class
    )
    fun handleBadRequestExceptions(
        ex: Exception
    ): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        log.warn("잘못된 요청: {} - {}", ex.javaClass.simpleName, ex.message)

        return null.toFailResponse(
            status = CommonErrorCode.INVALID_INPUT.httpStatus,
            imhereErrorCode = CommonErrorCode.INVALID_INPUT.imhereErrorCode,
            errorMessage = "잘못된 요청 형식입니다: ${ex.message}"
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        val rootCause = e.rootCause
        if (rootCause is ImHereBaseException) {
            return handleBaseException(rootCause, request)
        }

        log.warn("잘못된 HTTP 메시지: {}", e.message)

        return null.toFailResponse(
            status = CommonErrorCode.INVALID_HTTP_MESSAGE.httpStatus,
            imhereErrorCode = CommonErrorCode.INVALID_HTTP_MESSAGE.imhereErrorCode,
            errorMessage = CommonErrorCode.INVALID_HTTP_MESSAGE.errorMessage
        )
    }

    // --- 404 Not Found ---

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("리소스를 찾을 수 없음: {}", e.resourcePath)
        return null.toFailResponse(
            status = HttpStatus.NOT_FOUND,
            imhereErrorCode = CommonErrorCode.NOT_FOUND.imhereErrorCode,
            errorMessage = "잘못된 경로입니다: ${e.resourcePath}"
        )
    }

    // --- 405 Method Not Allowed ---

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.method)
        return null.toFailResponse(
            status = CommonErrorCode.METHOD_NOT_ALLOWED.httpStatus,
            imhereErrorCode = CommonErrorCode.METHOD_NOT_ALLOWED.imhereErrorCode,
            errorMessage = "지원하지 않는 메서드입니다: ${e.method}"
        )
    }

    // 인증/인가 실패(401/403)는 auth 모듈의 AuthAccessDeniedExceptionHandler가 소유한다(support→auth 순환 제거).

    // --- 409 Conflict ---
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleHttpMediaTypeNotSupportedException(e: DataIntegrityViolationException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("데이터 무결성 위반: {}", e.rootCause?.message ?: e.message)
        return null.toFailResponse(
            status = CommonErrorCode.CONFLICT.httpStatus,
            imhereErrorCode = CommonErrorCode.CONFLICT.imhereErrorCode,
            errorMessage = CommonErrorCode.CONFLICT.errorMessage
        )
    }

    // --- 415 Unsupported Media Type ---

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(e: HttpMediaTypeNotSupportedException): ResponseEntity<ApiResponse<Unit>> {
        log.warn("지원하지 않는 미디어 타입: {}", e.contentType)
        return null.toFailResponse(
            status = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.httpStatus,
            imhereErrorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.imhereErrorCode,
            errorMessage = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE.errorMessage
        )
    }

    // --- 500 Internal Server Error ---
    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Map<String, Any?>>> {
        log.error("예상하지 못한 오류 발생: ", e)

        return null.toFailResponse(
            status = CommonErrorCode.INTERNAL_SERVER_ERROR.httpStatus,
            imhereErrorCode = CommonErrorCode.INTERNAL_SERVER_ERROR.imhereErrorCode,
            errorMessage = CommonErrorCode.INTERNAL_SERVER_ERROR.errorMessage
        )
    }

    private fun maskedContext(context: Map<String, Any?>): Map<String, Any?> =
        context.mapValues { (key, value) ->
            val stringValue = value as? String
            when (key.lowercase()) {
                "email" -> SensitiveMasker.email(stringValue)
                "phone", "mobile", "receivernumber", "targetidentifier" -> SensitiveMasker.phone(stringValue)
                "token", "fcmtoken", "idtoken", "accesstoken", "refreshtoken" -> SensitiveMasker.token(stringValue)
                else -> value
            }
        }
}
