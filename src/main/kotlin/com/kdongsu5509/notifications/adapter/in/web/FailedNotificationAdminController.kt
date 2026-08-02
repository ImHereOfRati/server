package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.FailedNotificationResponse
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRedeliveryResponse
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/failed-notifications", version = "1")
class FailedNotificationAdminController(
    private val service: NotificationUseCase,
) {
    @GetMapping
    fun findAll(
        @RequestParam(defaultValue = "DEAD") status: NotificationStatus,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<List<FailedNotificationResponse>>> =
        service.findAll(status, page, size)
            .map(FailedNotificationResponse::from)
            .toOkResponse()

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<ApiResponse<FailedNotificationResponse>> =
        FailedNotificationResponse.from(service.findById(id)).toOkResponse()

    @PostMapping("/redelivery-jobs")
    fun redeliver(
        @RequestParam(required = false) count: Int?,
    ): ResponseEntity<ApiResponse<NotificationRedeliveryResponse>> =
        NotificationRedeliveryResponse(service.redeliverAll(count)).toOkResponse()

    @PostMapping("/{id}/redelivery-jobs")
    fun redeliverOne(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        service.redeliver(id)
        return Unit.toOkResponse()
    }

    @DeleteMapping("/{id}")
    fun discard(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        service.discard(id)
        return Unit.toOkResponse()
    }
}
