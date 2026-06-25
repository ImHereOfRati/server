package com.kdongsu5509.support.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class RabbitMQConfig {

    companion object {
        // ── 메인 Exchange ──────────────────────────────
        const val EXCHANGE_NAME: String = "imhere.noti.topic"

        // ── 메인 큐 / 라우팅 키 ────────────────────────
        const val FRIEND_QUEUE: String = "noti.queue.friend"
        const val SERVICE_QUEUE: String = "noti.queue.service"

        const val FRIEND_BINDING_PATTERN: String = "noti.friend.#"
        const val SERVICE_BINDING_PATTERN: String = "noti.service.#"

        // ── Producer 전송용 구체적 라우팅 키 ────────────────────────
        const val ROUTING_KEY_FRIEND_REQUEST_RECEIVED: String = "noti.friend.request.received"
        const val ROUTING_KEY_FRIEND_REQUEST_ACCEPTED: String = "noti.friend.request.accepted"
        const val ROUTING_KEY_LOCATION_SHARE: String = "noti.service.location.share"
        const val ROUTING_KEY_ARRIVAL_CONFIRMATION: String = "noti.service.location.arrival"
        const val ROUTING_KEY_DEPARTURE_CONFIRMATION: String = "noti.service.location.departure"
        const val ROUTING_KEY_TERMS_UPDATE: String = "noti.service.terms.update"
        const val ROUTING_KEY_DELIVERY_RESULT: String = "noti.service.delivery.result"
        const val ROUTING_KEY_DELIVERY_FAILED: String = "noti.service.delivery.failed"

        // ── Dead Letter Exchange / Queue ───────────────
        const val DLX_NAME: String = "imhere.noti.dlx"

        const val FRIEND_DLQ: String = "noti.queue.friend.dlq"
        const val SERVICE_DLQ: String = "noti.queue.service.dlq"

        // ── Retry 설정 ─────────────────────────────────
        const val RETRY_MAX_ATTEMPTS: Int = 3
        const val RETRY_INITIAL_INTERVAL_MS: Long = 1_000L
        const val RETRY_MULTIPLIER: Double = 2.0
        const val RETRY_MAX_INTERVAL_MS: Long = 8_000L
    }

    // ── Message Converter ──────────────────────────────────────────────────────

    @Bean
    fun jsonMessageConverter(): JacksonJsonMessageConverter {
        val jsonMapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()
        return JacksonJsonMessageConverter(jsonMapper)
    }

    // ── Main Exchange ──────────────────────────────────────────────────────────

    @Bean
    fun notiExchange(): TopicExchange = TopicExchange(EXCHANGE_NAME)

    // ── Dead Letter Exchange ───────────────────────────────────────────────────

    @Bean
    fun dlxExchange(): DirectExchange = DirectExchange(DLX_NAME)

    // ── Main Queues (DLX 연결 포함) ────────────────────────────────────────────

    @Bean
    fun friendQueue(): Queue =
        QueueBuilder.durable(FRIEND_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", FRIEND_DLQ)
            .build()

    @Bean
    fun serviceQueue(): Queue =
        QueueBuilder.durable(SERVICE_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", SERVICE_DLQ)
            .build()

    // ── Dead Letter Queues ─────────────────────────────────────────────────────

    @Bean
    fun friendDlq(): Queue = QueueBuilder.durable(FRIEND_DLQ).build()

    @Bean
    fun serviceDlq(): Queue = QueueBuilder.durable(SERVICE_DLQ).build()

    // ── Bindings ───────────────────────────────────────────────────────────────

    @Bean
    fun friendBinding(friendQueue: Queue, notiExchange: TopicExchange): Binding =
        BindingBuilder.bind(friendQueue).to(notiExchange).with(FRIEND_BINDING_PATTERN)

    @Bean
    fun serviceBinding(serviceQueue: Queue, notiExchange: TopicExchange): Binding =
        BindingBuilder.bind(serviceQueue).to(notiExchange).with(SERVICE_BINDING_PATTERN)

    @Bean
    fun friendDlqBinding(friendDlq: Queue, dlxExchange: DirectExchange): Binding =
        BindingBuilder.bind(friendDlq).to(dlxExchange).with(FRIEND_DLQ)

    @Bean
    fun serviceDlqBinding(serviceDlq: Queue, dlxExchange: DirectExchange): Binding =
        BindingBuilder.bind(serviceDlq).to(dlxExchange).with(SERVICE_DLQ)

    // ── Listener Container Factory (Retry 포함) ────────────────────────────────

    @Bean
    fun rabbitListenerContainerFactory(
        connectionFactory: ConnectionFactory,
        jsonMessageConverter: JacksonJsonMessageConverter
    ): SimpleRabbitListenerContainerFactory {
        val factory = SimpleRabbitListenerContainerFactory()
        factory.setConnectionFactory(connectionFactory)
        factory.setMessageConverter(jsonMessageConverter)
        // 재시도 소진 후 requeue하지 않음 → DLX 가 DLQ 로 라우팅
        factory.setDefaultRequeueRejected(false)

        val retryInterceptor = RetryInterceptorBuilder.stateless()
            .maxRetries(RETRY_MAX_ATTEMPTS)
            .backOffOptions(RETRY_INITIAL_INTERVAL_MS, RETRY_MULTIPLIER, RETRY_MAX_INTERVAL_MS)
            .recoverer(RejectAndDontRequeueRecoverer())
            .build()

        factory.setAdviceChain(retryInterceptor)

        return factory
    }
}
