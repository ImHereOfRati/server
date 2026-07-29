package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.application.service.FailedNotificationAdminService
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin/failed-notifications")
class FailedNotificationAdminPageController(
    private val service: FailedNotificationAdminService,
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("notifications", service.findAll(NotificationStatus.DEAD, 0, 100))
        return "admin/failed-notifications"
    }

    @PostMapping("/{id}/redelivery-jobs")
    fun redeliver(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes,
    ): String {
        service.redeliver(id)
        redirectAttributes.addFlashAttribute("message", "알림 $id 재발송을 요청했습니다.")
        return "redirect:/admin/failed-notifications"
    }

    @PostMapping("/{id}/discard")
    fun discard(
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes,
    ): String {
        service.discard(id)
        redirectAttributes.addFlashAttribute("message", "알림 $id 기록을 폐기했습니다.")
        return "redirect:/admin/failed-notifications"
    }
}
