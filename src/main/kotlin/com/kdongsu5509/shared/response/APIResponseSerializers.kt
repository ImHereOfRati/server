package com.kdongsu5509.shared.response

import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus

object APIResponseSerializers {

    private val jsonMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

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
