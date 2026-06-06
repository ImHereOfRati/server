package com.kdongsu5509.shared.response

import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus

/**
 * [HttpServletResponse]에 직접 API 응답을 작성하기 위한 유틸리티 객체 (필터 계층용)
 */
object APIResponseSerializers {

    private val jsonMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    /**
     * [HttpServletResponse]에 실패 응답을 JSON으로 직접 작성합니다.
     */
    fun writeErrorResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        imhereErrorCode: String,
        errorMessage: String
    ) {
        response.status = status.value()
        response.contentType = "application/json;charset=UTF-8"

        val body = ApiResponse.fail<Any>(
            imhereErrorCode = imhereErrorCode,
            errorMessage = errorMessage
        )

        response.writer.write(jsonMapper.writeValueAsString(body))
        response.writer.flush()
    }

    /**
     * [HttpServletResponse]에 성공 응답을 JSON으로 직접 작성합니다.
     */
    fun <T> writeSuccessResponse(
        response: HttpServletResponse,
        data: T? = null,
        message: String = "OK"
    ) {
        response.status = HttpStatus.OK.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val body = ApiResponse.success(data, message)

        response.writer.write(jsonMapper.writeValueAsString(body))
        response.writer.flush()
    }
}
