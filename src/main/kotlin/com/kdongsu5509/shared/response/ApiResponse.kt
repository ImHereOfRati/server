package com.kdongsu5509.shared.response

/**
 * 모든 API 응답의 공통 규격
 *
 * @param T 실제 응답 데이터의 타입
 */
data class ApiResponse<T>(
    val imhereResponseCode: String,
    val message: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T?, message: String = "OK"): ApiResponse<T> =
            ApiResponse(imhereResponseCode = "SUCCESS", message = message, data = data)

        fun <T> fail(imhereErrorCode: String, errorMessage: String, data: T? = null): ApiResponse<T> =
            ApiResponse(imhereResponseCode = imhereErrorCode, message = errorMessage, data = data)
    }
}
