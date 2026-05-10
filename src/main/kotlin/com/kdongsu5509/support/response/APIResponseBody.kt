package com.kdongsu5509.support.response

/**
 * 모든 API 응답의 공통 바디 규격 (Envelope Pattern)
 *
 * @param T 실제 응답 데이터의 타입
 */
data class APIResponseBody<T>(
    val code: String,
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> success(data: T?, message: String = "OK"): APIResponseBody<T> =
            APIResponseBody(code = "SUCCESS", message = message, data = data)

        fun <T> fail(globalCode: String, message: String, data: T? = null): APIResponseBody<T> =
            APIResponseBody(code = globalCode, message = message, data = data)
    }
}
