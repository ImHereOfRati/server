package com.kdongsu5509.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
class SecurityWhiteList(
    var whitelist: List<String> = emptyList(),
    var corsAllowedOrigins: List<String> = emptyList()
) {
    fun permitAllPaths(managementBasePath: String): List<String> {
        val normalizedBasePath = managementBasePath.trim().ifEmpty { "/actuator" }.let { path ->
            val withLeadingSlash = if (path.startsWith('/')) path else "/$path"
            withLeadingSlash.trimEnd('/')
        }

        val actuatorPaths = if (normalizedBasePath == "/") {
            listOf("/", "/**")
        } else {
            listOf(normalizedBasePath, "$normalizedBasePath/**")
        }

        return (whitelist + actuatorPaths).distinct()
    }
}
