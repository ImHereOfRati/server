package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationDispatcherUseCase
import com.kdongsu5509.notifications.application.service.strategy.NotificationDispatchStrategy
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service

@Service
class NotificationDispatcherService(
    strategies: List<NotificationDispatchStrategy>
) : NotificationDispatcherUseCase {

    private val strategyMap: Map<NotificationMethod, NotificationDispatchStrategy> =
        strategies.associateBy { it.notificationMethod }

    override fun dispatch(command: NotificationCommand) {
        val strategy = strategyMap[command.notificationMethod]
            ?: NotificationException.UNSUPPORTED_TARGET_TYPE.throwIt()

        strategy.dispatch(command)
    }

    override fun dispatchMultiple(command: MultipleNotificationCommand) {
        val strategy = strategyMap[command.notificationMethod]
            ?: NotificationException.UNSUPPORTED_TARGET_TYPE.throwIt()

        strategy.dispatchMultiple(command)
    }
}
