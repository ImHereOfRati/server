package com.kdongsu5509.shared.event

fun interface DomainEventPublisher {
    fun publish(event: DomainEvent)
}
