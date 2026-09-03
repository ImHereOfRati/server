package com.kdongsu5509.support.logger

import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper

@Component
class BodyMasker(private val jsonMapper: JsonMapper) {

    companion object {
        private val SENSITIVE_PATTERN =
            Regex(""""(password|pw|confirmPassword|secret|idToken|accessToken|refreshToken|token|fcmToken|email|phone|mobile|totp|apiKey|apiSecret)"\s*:\s*("[^"]*"|[^,}\s]+)""", RegexOption.IGNORE_CASE)
        private val SENSITIVE_KEYS = setOf(
            "password", "pw", "confirmpassword", "secret", "idtoken", "accesstoken",
            "refreshtoken", "token", "fcmtoken", "email", "phone", "mobile", "totp",
            "apikey", "apisecret"
        )
    }

    fun mask(body: String): String {
        return try {
            val jsonNode = jsonMapper.readTree(body)
            maskSensitiveFields(jsonNode)
            jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
        } catch (_: Exception) {
            body.replace(SENSITIVE_PATTERN) { match ->
                val key = match.groupValues[1]
                "\"$key\": \"*****\""
            }
        }
    }

    private fun maskSensitiveFields(node: JsonNode) {
        when {
            node.isObject -> {
                val objectNode = node as tools.jackson.databind.node.ObjectNode
                val fields = objectNode.propertyNames().toList()
                fields.forEach { key ->
                    if (key.lowercase() in SENSITIVE_KEYS) {
                        objectNode.put(key, "*****")
                    } else {
                        objectNode.get(key)?.let(::maskSensitiveFields)
                    }
                }
            }
            node.isArray -> node.forEach(::maskSensitiveFields)
        }
    }
}
