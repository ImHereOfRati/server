package com.kdongsu5509.support.external

data class AlertMessage(val content: String) {

    companion object {
        fun userError(code: String, message: String, context: RequestContext): AlertMessage =
            requestAlert("⚠️ User Error (4xx)", "Business / Input Error", code, message, context)

        fun abnormalAccess(code: String, message: String, context: RequestContext): AlertMessage =
            requestAlert("🚨 Abnormal Access (403)", "Authorization Denied", code, message, context)

        fun serverError(
            status: Int,
            traceId: String,
            method: String,
            uri: String,
            durationMs: Long,
            ip: String,
            formatted: String,
        ): AlertMessage = AlertMessage(
            """
            ## 🔥 Server Error ($status)
            **TraceId:** `$traceId`
            **$method** `$uri` — ${durationMs}ms
            **IP:** $ip

            ```
            $formatted
            ```
            """.trimIndent()
        )

        fun notificationDeliveryFailure(
            notificationId: Long?,
            target: String,
            type: String,
            errorType: String,
            errorMessage: String?,
        ): AlertMessage = AlertMessage(
            """
            ## 🚨 Notification Delivery Failure
            **NotificationId:** `$notificationId`
            **Target:** `$target`
            **Type:** `$type`
            **Error:** $errorType - $errorMessage
            """.trimIndent()
        )

        private fun requestAlert(
            header: String,
            type: String,
            code: String,
            message: String,
            context: RequestContext,
        ): AlertMessage = AlertMessage(
            """
            ## $header
            **Type:** $type
            **Code:** `$code`
            **Message:** $message
            **${context.method}** `${context.uri}`
            **IP:** ${context.clientIp}
            **User:** ${context.user}
            """.trimIndent()
        )
    }
}
