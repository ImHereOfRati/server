package com.kdongsu5509.support.logger

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.time.LocalDateTime
import java.util.*

data class AccessLog(
    val traceId: String,
    val method: String,
    val uri: String,
    val queryString: String?,
    val requestBody: String,
    val responseBody: String,
    val headers: Map<String, String>,
    val userAgent: String?,
    val remoteIp: String,
    val status: Int,
    val threadName: String,
    val requestAt: LocalDateTime,
    val responseAt: LocalDateTime,
    val durationMs: Long
) {
    companion object {
        private val objectMapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        private val SENSITIVE_HEADERS = setOf(
            "authorization", "cookie", "set-cookie", "x-auth-token", "proxy-authorization"
        )

        fun extractHeaders(headerMap: Map<String, List<String>>): Map<String, String> {
            return headerMap.entries.associate { (key, value) ->
                val lowerKey = key.lowercase()
                val maskedValue = if (SENSITIVE_HEADERS.contains(lowerKey)) {
                    "*****"
                } else {
                    value.joinToString(", ")
                }
                key to maskedValue
            }
        }
    }

    override fun toString(): String {
        return try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
        } catch (e: Exception) {
            "Log JSON Parsing Error"
        }
    }
}
