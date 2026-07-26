package com.kdongsu5509.maps

import org.springframework.http.HttpStatus

class NaverMapProxyException(
    val status: HttpStatus,
    val code: String,
    override val message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
