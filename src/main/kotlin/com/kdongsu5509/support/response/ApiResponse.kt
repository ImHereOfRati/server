package com.kdongsu5509.support.response

/**
 * 모든 API 응답의 공통 규격
 *
 * @param T 실제 응답 데이터의 타입
 */
data class ApiResponse<T>(
    val imhereErrorCode: String,
    val errorMessage: String,
    val data: T? = null
) {
    companion object {
        fun <T> success(data: T?, message: String = "OK"): ApiResponse<T> =
            ApiResponse(imhereErrorCode = "SUCCESS", errorMessage = message, data = data)

        fun <T> fail(imhereErrorCode: String, errorMessage: String, data: T? = null): ApiResponse<T> =
            ApiResponse(imhereErrorCode = imhereErrorCode, errorMessage = errorMessage, data = data)
    }
}
