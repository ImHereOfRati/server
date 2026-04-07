package com.kdongsu5509.support.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME: String = "imhere.noti.topic"

        const val FRIEND_QUEUE: String = "noti.queue.friend"
        const val SERVICE_QUEUE: String = "noti.queue.service"

        const val FRIEND_ROUTING_KEY: String = "noti.user.friend.#"
        const val SERVICE_ROUTING_KEY: String = "noti.service.#"
    }

    @Bean
    fun jsonMessageConverter(): JacksonJsonMessageConverter {
        val jsonMapper = JsonMapper.builder()
            .addModule(kotlinModule())
            .build()

        return JacksonJsonMessageConverter(jsonMapper)
    }

    @Bean
    fun notiExchange(): TopicExchange {
        return TopicExchange(EXCHANGE_NAME)
    }

    @Bean
    fun friendQueue(): Queue {
        return Queue(FRIEND_QUEUE)
    }

    @Bean
    fun serviceQueue(): Queue {
        return Queue(SERVICE_QUEUE)
    }

    @Bean
    fun friendBinding(friendQueue: Queue, notiExchange: TopicExchange): Binding {
        return BindingBuilder.bind(friendQueue).to(notiExchange).with(FRIEND_ROUTING_KEY)
    }

    @Bean
    fun serviceBinding(serviceQueue: Queue, notiExchange: TopicExchange): Binding {
        return BindingBuilder.bind(serviceQueue).to(notiExchange).with(SERVICE_ROUTING_KEY)
    }
}

