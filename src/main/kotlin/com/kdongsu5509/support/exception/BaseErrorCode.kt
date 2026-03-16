package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

interface BaseErrorCode {
    val status: HttpStatus
    val code: String
    val message: String
}
