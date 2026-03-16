package com.kdongsu5509.support.logger

import org.springframework.stereotype.Component

@Component
class ResponseLogBuilder(private val bodyMasker: BodyMasker) {

    fun build(log: AccessLog, sb: StringBuilder) {
        appendHeader(sb, log.status >= 400)
        appendStatusAndDuration(sb, log)
        appendResponseBody(sb, log)
        sb.append("--------------------------------------------------\n")
    }

    private fun appendHeader(sb: StringBuilder, isError: Boolean) {
        val header = if (isError) "❌ --- [Error Response] --------------------------\n"
        else "🚀 --- [Response] --------------------------\n"
        sb.append(header)
    }

    private fun appendStatusAndDuration(sb: StringBuilder, log: AccessLog) {
        sb.append("Status:   ").append(log.status).append("\n")
        sb.append("Duration: ").append(log.durationMs).append("ms\n")
    }

    private fun appendResponseBody(sb: StringBuilder, log: AccessLog) {
        if (log.responseBody.isNotEmpty()) {
            sb.append("Response: \n").append(bodyMasker.mask(log.responseBody)).append("\n")
        }
    }
}
