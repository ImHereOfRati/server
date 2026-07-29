package com.kdongsu5509.shared.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    override fun publish(event: DomainEvent) = applicationEventPublisher.publishEvent(event)
}
