package com.kdongsu5509.support.response

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

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
): ResponseEntity<APIResponseBody<T>> =
    ResponseEntity.status(status).body(
        APIResponseBody.fail(
            globalCode = globalCode,
            message = message ?: status.reasonPhrase,
            data = this
        )
    )
