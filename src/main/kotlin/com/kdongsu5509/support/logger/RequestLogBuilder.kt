package com.kdongsu5509.support.logger

import org.springframework.stereotype.Component

@Component
class RequestLogBuilder(private val bodyMasker: BodyMasker) {

    fun build(log: AccessLog, sb: StringBuilder) {
        appendHeader(sb, log.status >= 400)
        appendBasicInfo(sb, log)
        appendRequestBody(sb, log)
    }

    private fun appendHeader(sb: StringBuilder, isError: Boolean) {
        val header = if (isError) "🚨 --- [Error Request] --------------------------\n"
        else "✈️ --- [Request] ---------------------------\n"
        sb.append(header)
    }

    private fun appendBasicInfo(sb: StringBuilder, log: AccessLog) {
        sb.append("ID:       ").append(log.traceId).append("\n")
        sb.append("Method:   ").append(log.method).append("\n")
        appendUri(sb, log)
        sb.append("From:     ").append(log.remoteIp).append("\n")
        sb.append("User-Agent: ").append(log.userAgent).append("\n")
        sb.append("Headers:  ").append(log.headers).append("\n")
    }

    private fun appendUri(sb: StringBuilder, log: AccessLog) {
        sb.append("URI:      ").append(log.uri)
        if (!log.queryString.isNullOrEmpty()) {
            sb.append("?").append(log.queryString)
        }
        sb.append("\n")
    }

    private fun appendRequestBody(sb: StringBuilder, log: AccessLog) {
        if (log.requestBody.isNotEmpty()) {
            sb.append("Request:  \n").append(bodyMasker.mask(log.requestBody)).append("\n")
        }
    }
}
