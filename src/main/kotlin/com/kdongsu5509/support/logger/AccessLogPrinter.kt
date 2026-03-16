package com.kdongsu5509.support.logger

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AccessLogPrinter(
    private val messageSendPort: MessageSendPort,
    private val formatter: AccessLogFormatter
) {
    private val log = LoggerFactory.getLogger(AccessLogPrinter::class.java)

    fun print(accessLog: AccessLog, sendAlert: Boolean) {
        val formatted = formatter.format(accessLog)
        log.info(formatted)
        sendAlertIfNeeded(accessLog, formatted, sendAlert)
    }

    private fun sendAlertIfNeeded(accessLog: AccessLog, formatted: String, sendAlert: Boolean) {
        if (sendAlert && accessLog.status >= 400) {
            messageSendPort.sendMessage("## 🚨 HTTP Error\n\n```json\n$formatted\n```")
        }
    }
}
