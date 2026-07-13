package com.kdongsu5509.support.external

/**
 * Discord 경보 메시지 표현. 세 곳(사용자 오류·비정상 접근·서버 오류)에 흩어져 손조립되던 마크다운 템플릿을
 * 이 한 클래스로 응집한다(Extract Class). 포맷/필드 변경이 이 파일 한곳으로 끝난다(Shotgun Surgery 제거).
 */
data class DiscordAlertMessage(val content: String) {

    fun toDto(): DiscordMessageDto = DiscordMessageDto(content)

    companion object {
        /** 4xx 사용자/입력 오류 경보. */
        fun userError(code: String, message: String, context: RequestContext): DiscordAlertMessage =
            requestAlert("⚠️ User Error (4xx)", "Business / Input Error", code, message, context)

        /** 403 비정상 접근(인가 거부) 경보. */
        fun abnormalAccess(code: String, message: String, context: RequestContext): DiscordAlertMessage =
            requestAlert("🚨 Abnormal Access (403)", "Authorization Denied", code, message, context)

        /** 5xx 서버 오류 경보. */
        fun serverError(
            status: Int,
            traceId: String,
            method: String,
            uri: String,
            durationMs: Long,
            ip: String,
            formatted: String,
        ): DiscordAlertMessage = DiscordAlertMessage(
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

        private fun requestAlert(
            header: String,
            type: String,
            code: String,
            message: String,
            context: RequestContext,
        ): DiscordAlertMessage = DiscordAlertMessage(
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
