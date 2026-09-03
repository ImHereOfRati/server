package com.kdongsu5509.shared.response

import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity

fun <T> T?.toOkResponse(): ResponseEntity<ApiResponse<T>> =
    ResponseEntity.ok(ApiResponse.success(this))

fun <T> T?.toSuccessResponse(status: HttpStatus): ResponseEntity<ApiResponse<T>> =
    ResponseEntity.status(status)
        .body(
            ApiResponse.success(this, status.reasonPhrase)
        )

fun <T> T?.toFailResponse(
    status: HttpStatus,
    imhereErrorCode: String,
    errorMessage: String? = null,
    headers: HttpHeaders = HttpHeaders()
): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity.status(status).headers(headers).body(
        ApiResponse.fail(
            imhereErrorCode = imhereErrorCode,
            errorMessage = errorMessage ?: status.reasonPhrase,
            data = this
        )
    )
}
