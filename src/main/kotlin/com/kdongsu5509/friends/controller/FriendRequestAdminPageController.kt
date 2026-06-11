package com.kdongsu5509.friends.controller

import com.kdongsu5509.friends.service.FriendRequestService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.UUID

@Controller
@RequestMapping("/admin/friend-requests")
class FriendRequestAdminPageController(
    private val friendRequestService: FriendRequestService
) {
    @GetMapping
    fun page(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val result = friendRequestService.findAll(PageRequest.of(page, size))
        model.addAttribute("requests", result.content)
        model.addAttribute("hasNext", result.hasNext())
        return "admin/friend-requests"
    }

    @PostMapping("/{id}/delete")
    fun delete(
        @PathVariable id: UUID,
        redirectAttributes: RedirectAttributes
    ): String {
        friendRequestService.deleteById(id)
        redirectAttributes.addFlashAttribute("message", "친구 요청을 삭제했습니다.")
        return "redirect:/admin/friend-requests"
    }
}
