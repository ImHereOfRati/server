package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

interface ImHereBaseErrorCode {
    val httpStatus: HttpStatus
    val imhereErrorCode: String
    val errorMessage: String

    fun isCritical(): Boolean = httpStatus.is5xxServerError
}
