package com.kdongsu5509.support.logger

import com.kdongsu5509.support.external.AlertChannel
import com.kdongsu5509.support.external.AlertMessage
import com.kdongsu5509.support.external.ErrorAlertPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AccessLogPrinter(
    private val errorAlertPort: ErrorAlertPort,
    private val formatter: AccessLogFormatter
) {

    private val log = LoggerFactory.getLogger(AccessLogPrinter::class.java)

    fun print(accessLog: AccessLog, sendAlert: Boolean) {
        val formatted = formatter.format(accessLog)
        log.info(formatted)
        sendAlertIfNeeded(accessLog, formatted, sendAlert)
    }

    private fun sendAlertIfNeeded(accessLog: AccessLog, formatted: String, sendAlert: Boolean) {
        if (!sendAlert || accessLog.status < 500) return

        errorAlertPort.send(AlertChannel.SERVER_ERROR, build5xxAlert(accessLog, formatted))
    }

    private fun build5xxAlert(accessLog: AccessLog, formatted: String): AlertMessage {
        val uri = accessLog.uri + (accessLog.queryString?.let { "?$it" } ?: "")
        return AlertMessage.serverError(
            status = accessLog.status,
            traceId = accessLog.traceId,
            method = accessLog.method,
            uri = uri,
            durationMs = accessLog.durationMs,
            ip = accessLog.remoteIp,
            formatted = formatted,
        )
    }
}
