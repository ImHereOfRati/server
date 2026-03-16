package com.kdongsu5509.support.logger

import org.springframework.stereotype.Component

@Component
class AccessLogFormatter(
    private val requestBuilder: RequestLogBuilder,
    private val responseBuilder: ResponseLogBuilder
) {
    fun format(accessLog: AccessLog): String {
        val sb = StringBuilder("\n")
        requestBuilder.build(accessLog, sb)
        responseBuilder.build(accessLog, sb)
        return sb.toString()
    }
}
