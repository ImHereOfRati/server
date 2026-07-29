package com.kdongsu5509.notifications.adapter.`in`.event

import com.kdongsu5509.notifications.application.service.NotificationDeliveryService
import com.kdongsu5509.notifications.event.NotificationRequested
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class NotificationRequestedEventListener(
    private val deliveryService: NotificationDeliveryService,
) {
    @ApplicationModuleListener
    fun handle(event: NotificationRequested) =
        deliveryService.deliver(event)
}
