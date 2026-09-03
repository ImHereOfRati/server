package com.kdongsu5509.support.config

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy
import ch.qos.logback.core.util.FileSize
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test & !prod")
class ErrorLogAppenderConfig {

    companion object {
        private const val APPENDER_NAME = "ERROR_FILE"
        private const val LOG_FILE_PATH = "logs/imhere-error.log"
        private const val LOG_FILE_PATTERN = "logs/imhere-error.%d{yyyy-MM-dd}.%i.log.gz"

        private const val MAX_FILE_SIZE = "50MB"
        private const val TOTAL_SIZE_CAP = "500MB"
        private const val MAX_HISTORY = 90

        private const val LOG_LEVEL = "WARN"
        private const val LOG_PATTERN =
            "%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level" +
                    " [traceId=%X{traceId:-}]" +
                    " [%X{method:-} %X{uri:-} -> %X{status:-} %X{durationMs:-}ms]" +
                    " %logger{40} - %message%n%exception"
    }

    @PostConstruct
    fun addErrorFileAppender() {
        val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return

        val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)
        if (rootLogger.getAppender(APPENDER_NAME) != null) return

        val encoder = PatternLayoutEncoder().apply {
            this.context = context
            this.pattern = LOG_PATTERN
            start()
        }

        val rollingPolicy = SizeAndTimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            this.fileNamePattern = LOG_FILE_PATTERN
            this.setMaxFileSize(FileSize.valueOf(MAX_FILE_SIZE))
            this.maxHistory = MAX_HISTORY
            this.setTotalSizeCap(FileSize.valueOf(TOTAL_SIZE_CAP))
        }

        val appender = RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent>().apply {
            this.context = context
            this.name = APPENDER_NAME
            this.file = LOG_FILE_PATH
            this.encoder = encoder
            addFilter(ThresholdFilter().apply {
                setLevel(LOG_LEVEL)
                start()
            })
            rollingPolicy.setParent(this)
            rollingPolicy.start()
            this.rollingPolicy = rollingPolicy
            start()
        }

        val asyncAppender = AsyncAppender().apply {
            this.context = context
            name = "ASYNC_$APPENDER_NAME"
            queueSize = 256
            discardingThreshold = 0
            isNeverBlock = true
            addAppender(appender)
            start()
        }

        rootLogger.addAppender(asyncAppender)
    }
}
