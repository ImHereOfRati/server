package com.kdongsu5509.support.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * 성공 응답 (200 OK)을 반환합니다.
 */
fun <T> T?.toOkResponse(): ResponseEntity<ApiResponse<T>> =
    ResponseEntity.ok(ApiResponse.success(this))

/**
 * 성공 응답을 지정된 상태 코드와 함께 반환합니다.
 */
fun <T> T?.toSuccessResponse(status: HttpStatus): ResponseEntity<ApiResponse<T>> =
    ResponseEntity.status(status).body(ApiResponse.success(this, status.reasonPhrase))

/**
 * 실패 응답을 반환합니다.
 */
fun <T> T?.toFailResponse(
    status: HttpStatus,
    imhereErrorCode: String,
    errorMessage: String? = null
): ResponseEntity<ApiResponse<T>> {
    return ResponseEntity.status(status).body(
        ApiResponse.fail(
            imhereErrorCode = imhereErrorCode,
            errorMessage = errorMessage ?: status.reasonPhrase,
            data = this
        )
    )
}
