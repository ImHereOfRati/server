package com.kdongsu5509.support.exception.handler

import com.kdongsu5509.support.exception.ErrorReason
import com.kdongsu5509.support.response.APIResponseBody
import com.kdongsu5509.support.response.toFailResponse
import org.springframework.http.ResponseEntity

abstract class AbstractExceptionHandler {

    protected fun createErrorResponse(
        reason: ErrorReason,
        message: String?,
        metadata: Map<String, Any?> = emptyMap()
    ): ResponseEntity<APIResponseBody<Map<String, Any?>>> {
        return metadata.toFailResponse(
            status = reason.httpStatus,
            code = reason.globalCode, // ErrorReason에 정의된 전역 비즈니스 코드(GLOBAL-XXX) 주입
            message = message
        )
    }

}
