package com.common.testUtil

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

abstract class TestRabbitMQContainer {

    companion object {
        private const val RABBIT_MQ_PORT = 5672
        private const val RABBIT_MQ_DOCKER_IMAGE = "rabbitmq:4.3.0-rc.0-management-alpine"

        val rabbitMqContainer = GenericContainer(DockerImageName.parse(RABBIT_MQ_DOCKER_IMAGE))
            .withExposedPorts(RABBIT_MQ_PORT)
            .apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.rabbitmq.host") { rabbitMqContainer.host }
            registry.add("spring.rabbitmq.port") { rabbitMqContainer.getMappedPort(RABBIT_MQ_PORT) }
            registry.add("spring.rabbitmq.virtual-host") { "/" }
            registry.add("spring.rabbitmq.username") { "guest" }
            registry.add("spring.rabbitmq.password") { "guest" }
        }
    }
}
