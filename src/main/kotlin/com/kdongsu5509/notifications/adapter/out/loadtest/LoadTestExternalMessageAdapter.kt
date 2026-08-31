package com.kdongsu5509.notifications.adapter.out.loadtest

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.MessageSendResult
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.support.config.LoadTestProviderProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("loadtest")
class LoadTestExternalMessageAdapter(
    properties: LoadTestProviderProperties,
) : ExternalMessagePort {
    private val support = LoadTestProviderSupport(properties)

    override fun send(sms: SMS): MessageSendResult {
        support.delay()
        if (!support.shouldFail()) return MessageSendResult.success("loadtest-sms-${UUID.randomUUID()}")
        return when (support.failureMode()) {
            LoadTestProviderProperties.FailureMode.UNKNOWN ->
                MessageSendResult.unknown("load-test SMS provider unknown result")
            else -> MessageSendResult.fail("load-test SMS provider rejection")
        }
    }

    override fun sendMultiple(multiSMS: List<SMS>): List<MessageSendResult> = multiSMS.map(::send)

    override fun findStatus(providerMessageId: String): MessageSendResult? = null
}
