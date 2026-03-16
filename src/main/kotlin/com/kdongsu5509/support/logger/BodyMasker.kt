package com.kdongsu5509.support.logger

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component

@Component
class BodyMasker(private val objectMapper: ObjectMapper) {

    fun mask(body: String): String {
        val formatted = formatJson(body)
        return maskSensitiveFields(formatted)
    }

    private fun formatJson(body: String): String {
        return try {
            val jsonNode = objectMapper.readTree(body)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
        } catch (e: Exception) {
            body
        }
    }

    private fun maskSensitiveFields(json: String): String {
        return json.replace(
            Regex("\"(password|pw|confirmPassword|secret)\"\\s*:\\s*\"[^\"]+\""),
            "\"$1\": \"*****\""
        )
    }
}
