package com.kdongsu5509.support.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * 성공 응답 (200 OK)을 반환합니다.
 */
fun <T> T?.toOkResponse(): ResponseEntity<APIResponseBody<T>> =
    ResponseEntity.ok(APIResponseBody.success(this))

/**
 * 성공 응답을 지정된 상태 코드와 함께 반환합니다.
 */
fun <T> T?.toSuccessResponse(status: HttpStatus): ResponseEntity<APIResponseBody<T>> =
    ResponseEntity.status(status).body(APIResponseBody.success(this, status.reasonPhrase))

/**
 * 실패 응답을 반환합니다.
 */
fun <T> T?.toFailResponse(
    status: HttpStatus,
    globalCode: String,
    message: String? = null
): ResponseEntity<APIResponseBody<T>> {
    return ResponseEntity.status(status).body(
        APIResponseBody.fail(
            globalCode = globalCode,
            message = message ?: status.reasonPhrase,
            data = this
        )
    )
}
