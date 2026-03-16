package com.kdongsu5509.support.common.dto

import org.springframework.http.HttpStatus

class APIResponse<T> private constructor(
    val code: Int,
    val message: String?,
    val data: T?
) {
    companion object {
        fun <T> success(data: T): APIResponse<T> {
            return APIResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.reasonPhrase,
                data
            )
        }

        fun <T> successWithHttpStatusCode(httpStatusCode: Int, data: T): APIResponse<T> {
            return APIResponse(
                httpStatusCode,
                HttpStatus.OK.reasonPhrase,
                data
            )
        }

        fun success(): APIResponse<Unit> {
            return APIResponse(
                HttpStatus.OK.value(),
                "OK",
                null
            )
        }

        fun <T> fail(code: Int, message: String?): APIResponse<T?> {
            return APIResponse(code, message, null)
        }

        fun <T> fail(code: Int, message: String?, data: T): APIResponse<T> {
            return APIResponse(code, message, data)
        }
    }
}
