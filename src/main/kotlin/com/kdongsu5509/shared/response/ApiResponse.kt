package com.kdongsu5509.shared.response

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
