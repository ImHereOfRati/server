package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.application.service.DlqAdminService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin/dead-letter-queues")
class DlqAdminPageController(
    private val dlqAdminService: DlqAdminService
) {
    @GetMapping
    fun page(model: Model): String {
        model.addAttribute("queues", dlqAdminService.getAllDlqInfo())
        return "admin/dlq"
    }

    @PostMapping("/{queueName}/replay-jobs")
    fun replay(
        @PathVariable queueName: String,
        @RequestParam(required = false) count: Int?,
        redirectAttributes: RedirectAttributes
    ): String {
        val result = dlqAdminService.replayMessages(queueName, count)
        redirectAttributes.addFlashAttribute("message", "${result.queueName}에서 ${result.replayedCount}건을 재처리했습니다.")
        return "redirect:/admin/dead-letter-queues"
    }

    @PostMapping("/{queueName}/messages/purge")
    fun purge(
        @PathVariable queueName: String,
        redirectAttributes: RedirectAttributes
    ): String {
        dlqAdminService.purgeQueue(queueName)
        redirectAttributes.addFlashAttribute("message", "$queueName 큐를 비웠습니다.")
        return "redirect:/admin/dead-letter-queues"
    }
}
