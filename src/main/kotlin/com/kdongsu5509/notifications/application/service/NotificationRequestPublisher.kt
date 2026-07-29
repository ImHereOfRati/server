package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.event.NotificationRequested
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationRequestPublisher(
    private val eventPublisher: DomainEventPublisher,
) {
    @Transactional
    fun publish(command: NotificationCommand) {
        eventPublisher.publish(NotificationRequested.from(command))
    }

    @Transactional
    fun publish(command: MultipleNotificationCommand) {
        command.toSingles().forEach { publish(it) }
    }
}
